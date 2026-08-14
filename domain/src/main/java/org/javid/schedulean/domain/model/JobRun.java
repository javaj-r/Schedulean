package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.event.*;
import org.javid.schedulean.domain.exception.JobExecutionException;
import org.javid.schedulean.domain.exception.JobTimeoutException;
import org.javid.schedulean.domain.valueobject.*;
import org.javid.schedulean.domain.valueobject.enums.AttemptStatus;
import org.javid.schedulean.domain.valueobject.enums.RecoveryReason;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JobRun {

    public static final String OCCURRED_AT_CANNOT_BE_NULL = "occurredAt cannot be null";
    public static final String ATTEMPT_CANNOT_BE_NULL = "attempt cannot be null";
    private final JobRunId id;
    private final JobId jobId;
    private final NodeInstanceId createdByNodeId;
    private RunStatus status;
    private final Instant scheduledAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Long durationMs;
    private int attemptCount;
    private String errorType;
    private String errorMessage;
    private String resultPayload;
    private NodeInstanceId executingNodeId;
    private Instant lastHeartbeat;
    private final TraceContext traceContext;

    /**
     * The ID of the chain this run belongs to, if any.
     * Null means this run is not part of a chain.
     */
    private final ChainId chainId;

    /**
     * The ID of the batch this run belongs to, if any.
     * Null means this run is not part of a batch.
     */
    private final JobBatchId batchId;

    private final List<JobAttempt> attempts = new ArrayList<>();
    private final List<DomainEvent> events = new ArrayList<>();

    /**
     * Primary package-private constructor for NEW runs.
     * Enforces strict null rejection and sets the default PENDING state.
     */
    JobRun(
            JobRunId id,
            JobId jobId,
            NodeInstanceId createdByNodeId,
            Instant scheduledAt,
            TraceContext traceContext,
            ChainId chainId,
            JobBatchId batchId) {

        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.jobId = Objects.requireNonNull(jobId, "jobId cannot be null");
        this.createdByNodeId = Objects.requireNonNull(createdByNodeId, "createdByNodeId cannot be null");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt cannot be null");
        this.traceContext = Objects.requireNonNull(traceContext, "traceContext cannot be null. Use TraceContext.empty()");
        this.chainId = chainId; // Nullable
        this.batchId = batchId; // Nullable
        this.status = RunStatus.PENDING; // State machine starts at PENDING
    }

    /**
     * Package-private constructor for RECONSTITUTING runs from persistence.
     */
    JobRun(JobRunSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot cannot be null");

        this(
                snapshot.id(),
                snapshot.jobId(),
                snapshot.createdByNodeId(),
                snapshot.scheduledAt(),
                snapshot.traceContext(),
                snapshot.chainId(),
                snapshot.batchId()
        );

        // Enforce business rules for reconstituted fields
        this.status = Objects.requireNonNull(snapshot.status(), "status cannot be null");

        // Override default PENDING state with historical state
        this.startedAt = snapshot.startedAt();
        this.finishedAt = snapshot.finishedAt();
        this.durationMs = snapshot.durationMs();
        this.attemptCount = snapshot.attemptCount();
        this.errorType = snapshot.errorType();
        this.errorMessage = snapshot.errorMessage();
        this.resultPayload = snapshot.resultPayload();
        this.executingNodeId = snapshot.executingNodeId();
        this.lastHeartbeat = snapshot.lastHeartbeat();

        // snapshot.attempts() is already defensively copied in JobRunSnapshot
        this.attempts.addAll(snapshot.attempts());

        // Validate persisted-state consistency at this boundary
        validateReconstitutedState();
    }

    private void validateReconstitutedState() {
        if (attemptCount != attempts.size()) {
            throw new JobExecutionException("Reconstituted state invalid: attemptCount does not match attempts list size.");
        }

        if (status == RunStatus.PENDING) {
            if (startedAt != null || finishedAt != null || executingNodeId != null || lastHeartbeat != null) {
                throw new JobExecutionException("Reconstituted state invalid: PENDING run has execution fields.");
            }
        } else if (status == RunStatus.RUNNING) {
            if (startedAt == null || executingNodeId == null || lastHeartbeat == null || finishedAt != null) {
                throw new JobExecutionException("Reconstituted state invalid: RUNNING run is missing required execution fields or has finishedAt.");
            }
        } else if (status == RunStatus.RECOVERING) {
            if (startedAt == null || executingNodeId == null || lastHeartbeat == null) {
                throw new JobExecutionException("Reconstituted state invalid: RECOVERING run is missing required recovery fields.");
            }
        } else if (status == RunStatus.SUCCESS || status == RunStatus.FAILED || status == RunStatus.TIMED_OUT) {
            if (startedAt == null || finishedAt == null) {
                throw new JobExecutionException("Reconstituted state invalid: Terminal run is missing startedAt or finishedAt.");
            }
            if (durationMs != null && durationMs < 0) {
                throw new JobExecutionException("Reconstituted state invalid: Terminal run has negative duration.");
            }
        }
    }

    public void markStarted(NodeInstanceId nodeId, Instant occurredAt) {
        Objects.requireNonNull(nodeId, "nodeId cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.PENDING) {
            throw new JobExecutionException("Cannot start a run that is not PENDING. Current state: " + status);
        }
        this.status = RunStatus.RUNNING;
        this.startedAt = occurredAt;
        this.executingNodeId = nodeId;
        this.lastHeartbeat = occurredAt;

        events.add(new JobRunStarted(jobId, id, nodeId, occurredAt));
    }

    public void updateHeartbeat(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot update heartbeat on a non-running job");
        }
        this.lastHeartbeat = occurredAt;
    }

    public JobAttempt startAttempt(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot start an attempt when run is not RUNNING. Current state: " + status);
        }
        int n = attempts.size() + 1;
        var a = new JobAttempt(n, occurredAt);
        attempts.add(a);
        this.attemptCount = n;
        return a;
    }

    public void markAttemptSucceeded(JobAttempt attempt, Instant occurredAt) {
        Objects.requireNonNull(attempt, ATTEMPT_CANNOT_BE_NULL);
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot mark attempt succeeded when run is not RUNNING");
        }
        if (!attempts.contains(attempt) || attempt.status() != AttemptStatus.RUNNING) {
            throw new JobExecutionException("Invalid attempt provided for success");
        }
        attempt.succeed(occurredAt);

        events.add(new JobAttemptSucceeded(jobId, id, attempt.number(), occurredAt));
    }

    public void markAttemptFailed(JobAttempt attempt, Throwable t, Instant occurredAt) {
        Objects.requireNonNull(attempt, ATTEMPT_CANNOT_BE_NULL);
        Objects.requireNonNull(t, "throwable cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot mark attempt failed when run is not RUNNING");
        }
        if (!attempts.contains(attempt) || attempt.status() != AttemptStatus.RUNNING) {
            throw new JobExecutionException("Invalid attempt provided for failure");
        }
        attempt.fail(t, occurredAt);

        events.add(new JobAttemptFailed(jobId, id, attempt.number(), t.getClass().getName(), occurredAt));
    }

    public void markAttemptTimedOut(JobAttempt attempt, Instant occurredAt) {
        Objects.requireNonNull(attempt, ATTEMPT_CANNOT_BE_NULL);
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot mark attempt timed out when run is not RUNNING");
        }
        if (!attempts.contains(attempt) || attempt.status() != AttemptStatus.RUNNING) {
            throw new JobExecutionException("Invalid attempt provided for timeout");
        }
        attempt.timeout(occurredAt);
    }

    public void markAttemptLockAcquired(JobAttempt attempt, Instant occurredAt) {
        Objects.requireNonNull(attempt, ATTEMPT_CANNOT_BE_NULL);
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot mark lock acquired when run is not RUNNING");
        }
        if (!attempts.contains(attempt) || attempt.status() != AttemptStatus.RUNNING) {
            throw new JobExecutionException("Invalid attempt provided for lock acquisition");
        }
        attempt.markLockAcquired(occurredAt);
    }

    public void succeed(String resultPayload, Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot succeed a run that is not RUNNING. Current state: " + status);
        }
        this.status = RunStatus.SUCCESS;
        this.finishedAt = occurredAt;
        if (resultPayload != null) this.resultPayload = resultPayload;
        this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();

        events.add(new JobRunSucceeded(jobId, id, occurredAt));
    }

    public void fail(Throwable throwable, Instant occurredAt) {
        Objects.requireNonNull(throwable, "throwable cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot fail a run that is not RUNNING. Current state: " + status);
        }
        this.status = RunStatus.FAILED;
        this.finishedAt = occurredAt;
        this.errorType = throwable.getClass().getName();
        this.errorMessage = truncate(throwable.getMessage(), 4000);
        this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();

        events.add(new JobRunFailed(jobId, id, throwable.getClass().getName(), occurredAt));
    }

    public void timeout(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot timeout a run that is not RUNNING. Current state: " + status);
        }
        this.status = RunStatus.TIMED_OUT;
        this.finishedAt = occurredAt;
        this.errorType = JobTimeoutException.class.getName();
        this.errorMessage = "Job exceeded its configured timeout";
        this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();

        events.add(new JobTimedOut(jobId, id, occurredAt));
    }

    public void beginRecovery(NodeInstanceId recoveringNodeId, Instant occurredAt) {
        Objects.requireNonNull(recoveringNodeId, "recoveringNodeId cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);

        // Strictly limit to RUNNING. PENDING dispatches are handled by markAbandoned.
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot begin recovery on a run that is not RUNNING. Current state: " + status);
        }
        this.status = RunStatus.RECOVERING;
        this.executingNodeId = recoveringNodeId;
        this.lastHeartbeat = occurredAt;
        events.add(new JobRecoveryStarted(jobId, id, recoveringNodeId, occurredAt));
    }

    /**
     * Explicit transition for abandoned dispatches (stale PENDING runs).
     * Bypasses RECOVERING state and goes straight to FAILED.
     * Sets startedAt to prevent validation errors on reconstitution.
     */
    public void markAbandoned(Instant occurredAt, RecoveryReason reason) {
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");

        if (this.status != RunStatus.PENDING) {
            throw new JobExecutionException("Cannot mark a run as abandoned if it is not PENDING. Current state: " + status);
        }
        this.status = RunStatus.FAILED;
        this.startedAt = occurredAt;
        this.finishedAt = occurredAt;
        this.errorType = "JobAbandoned";
        this.errorMessage = reason.name();
        this.durationMs = 0L;
        events.add(new JobReclaimed(jobId, id, reason.name(), occurredAt));
    }

    /**
     * Strict transition limit. Reclaim only allowed from durable RECOVERING state
     */
    public void reclaim(RecoveryReason reason, Instant occurredAt) {
        Objects.requireNonNull(reason, "reason cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);

        if (this.status != RunStatus.RECOVERING) {
            throw new JobExecutionException("Cannot reclaim a run that is not RECOVERING. Current state: " + status);
        }
        this.status = RunStatus.FAILED;
        this.finishedAt = occurredAt;
        this.errorType = "JobReclaimed";
        this.errorMessage = reason.name();
        this.durationMs = startedAt != null ? finishedAt.toEpochMilli() - startedAt.toEpochMilli() : 0L;

        events.add(new JobReclaimed(jobId, id, reason.name(), occurredAt));
    }

    /**
     * Releases the distributed lock associated with this run.
     * This is typically called by the application service after the run terminates,
     * or by the zombie recovery service when reclaiming a stuck run.
     */
    public void releaseLock(LockName lockName, Instant occurredAt) {
        Objects.requireNonNull(lockName, "lockName cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);

        events.add(new LockReleased(jobId, lockName, occurredAt));
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(events);
        events.clear();
        return copy;
    }

    // Getters
    public JobRunId id() {
        return id;
    }

    public JobId jobId() {
        return jobId;
    }

    public NodeInstanceId createdByNodeId() {
        return createdByNodeId;
    }

    public RunStatus status() {
        return status;
    }

    public Instant scheduledAt() {
        return scheduledAt;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public Long durationMs() {
        return durationMs;
    }

    public int attemptCount() {
        return attemptCount;
    }

    public String errorType() {
        return errorType;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public String resultPayload() {
        return resultPayload;
    }

    public NodeInstanceId executingNodeId() {
        return executingNodeId;
    }

    public Instant lastHeartbeat() {
        return lastHeartbeat;
    }

    public TraceContext traceContext() {
        return traceContext;
    }

    public ChainId chainId() {
        return chainId;
    }

    public JobBatchId batchId() {
        return batchId;
    }

    public boolean isTerminal() {
        return status == RunStatus.SUCCESS || status == RunStatus.FAILED || status == RunStatus.TIMED_OUT;
    }

    public List<JobAttempt> attempts() {
        return Collections.unmodifiableList(attempts);
    }
}