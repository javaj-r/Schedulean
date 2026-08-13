package org.javid.schedulean.domain.valueobject;

import org.javid.schedulean.domain.model.JobAttempt;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A Value Object representing the persisted state of a JobRun.
 * Used by Repository adapters to pass data to the Factory for reconstitution.
 */
public record JobRunSnapshot(
        JobRunId id,
        JobId jobId,
        NodeInstanceId createdByNodeId,
        Instant scheduledAt,
        TraceContext traceContext,
        ChainId chainId,
        JobBatchId batchId,
        RunStatus status,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        int attemptCount,
        String errorType,
        String errorMessage,
        String resultPayload,
        NodeInstanceId executingNodeId,
        Instant lastHeartbeat,
        List<JobAttempt> attempts) {
    public JobRunSnapshot {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(createdByNodeId, "createdByNodeId cannot be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt cannot be null");
        Objects.requireNonNull(traceContext, "traceContext cannot be null");
        Objects.requireNonNull(status, "status cannot be null");

        // Defensive copy of mutable list
        attempts = attempts == null ? List.of() : List.copyOf(attempts);

        // Validate primitive wrappers if non-null
        if (durationMs != null && durationMs < 0) throw new IllegalArgumentException("durationMs cannot be negative");
        if (attemptCount < 0) throw new IllegalArgumentException("attemptCount cannot be negative");
    }
}