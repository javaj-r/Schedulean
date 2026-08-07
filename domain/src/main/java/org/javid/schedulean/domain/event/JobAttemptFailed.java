package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;

import java.time.Instant;
import java.util.Objects;

public record JobAttemptFailed(JobId jobId, JobRunId runId, int attempt, String errorType, Instant occurredAt) implements JobDomainEvent {
    public JobAttemptFailed {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }
        if (errorType == null || errorType.isBlank()) {
            throw new IllegalArgumentException("errorType cannot be null or blank");
        }
    }
}