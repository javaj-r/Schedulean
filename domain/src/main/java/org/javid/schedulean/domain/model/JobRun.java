package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.event.*;
import org.javid.schedulean.domain.exception.JobExecutionException;
import org.javid.schedulean.domain.exception.JobTimeoutException;
import org.javid.schedulean.domain.valueobject.*;
import org.javid.schedulean.domain.valueobject.enums.AttemptStatus;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * NOTE ON EVENTS: To keep the aggregate focused purely on state transitions,
 * JobRun and JobAttempt domain events (e.g., JobRunStarted, JobAttemptFailed)
 * are emitted exclusively by the application service (ExecuteJobService)
 * which wraps these state transitions.
 */
public class JobRun {

    public static final String OCCURRED_AT_CANNOT_BE_NULL = "occurredAt cannot be null";
    private final JobRunId id;
    private final JobId jobId;
    private final NodeInstanceId instanceId;
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
    private final ChainId chainId;
    private final JobBatchId batchId;

    private final List<JobAttempt> attempts = new ArrayList<>();
    private final List<DomainEvent> events = new ArrayList<>();

    public JobRun(JobRunId id,
                  JobId jobId,
                  NodeInstanceId instanceId,
                  Instant scheduledAt,
                  TraceContext traceContext,
                  ChainId chainId,
                  JobBatchId batchId) {

        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.jobId = Objects.requireNonNull(jobId, "jobId cannot be null");
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId cannot be null");
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt cannot be null");
        this.traceContext = Objects.requireNonNull(traceContext, "traceContext cannot be null. Use TraceContext.empty()");
        this.chainId = chainId; // Nullable
        this.batchId = batchId; // Nullable
        this.status = RunStatus.PENDING; // State machine starts at PENDING
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

    public void markAttemptSucceeded(JobAttempt a, Instant occurredAt) {
        Objects.requireNonNull(a, "attempt cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot mark attempt succeeded when run is not RUNNING");
        }
        if (!attempts.contains(a) || a.status() != AttemptStatus.RUNNING) {
            throw new JobExecutionException("Invalid attempt provided for success");
        }
        a.succeed(occurredAt);

        events.add(new JobAttemptSucceeded(jobId, id, a.number(), occurredAt));
    }

    public void markAttemptFailed(JobAttempt a, Throwable t, Instant occurredAt) {
        Objects.requireNonNull(a, "attempt cannot be null");
        Objects.requireNonNull(t, "throwable cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot mark attempt failed when run is not RUNNING");
        }
        if (!attempts.contains(a) || a.status() != AttemptStatus.RUNNING) {
            throw new JobExecutionException("Invalid attempt provided for failure");
        }
        a.fail(t, occurredAt);

        events.add(new JobAttemptFailed(jobId, id, a.number(), t.getClass().getName(), occurredAt));
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

    public void fail(Throwable t, Instant occurredAt) {
        Objects.requireNonNull(t, "throwable cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.status != RunStatus.RUNNING) {
            throw new JobExecutionException("Cannot fail a run that is not RUNNING. Current state: " + status);
        }
        this.status = RunStatus.FAILED;
        this.finishedAt = occurredAt;
        this.errorType = t.getClass().getName();
        this.errorMessage = truncate(t.getMessage(), 4000);
        this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();

        events.add(new JobRunFailed(jobId, id, t.getClass().getName(), occurredAt));
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

    /**
     * Reclaims a run that was orphaned or zombie.
     */
    public void reclaim(String reason, Instant occurredAt) {
        Objects.requireNonNull(reason, "reason cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (reason.isBlank()) throw new JobExecutionException("reason cannot be blank");

        if (this.status != RunStatus.RUNNING && this.status != RunStatus.PENDING) {
            throw new JobExecutionException("Cannot reclaim a run that is already terminal. Current state: " + status);
        }
        this.status = RunStatus.FAILED;
        this.finishedAt = occurredAt;
        this.errorType = "JobReclaimed";
        this.errorMessage = reason;
        this.durationMs = startedAt != null ? finishedAt.toEpochMilli() - startedAt.toEpochMilli() : 0L;

        events.add(new JobReclaimed(jobId, id, reason, occurredAt));
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

    public NodeInstanceId instanceId() {
        return instanceId;
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