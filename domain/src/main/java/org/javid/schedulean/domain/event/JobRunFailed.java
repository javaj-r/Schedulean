package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;

import java.time.Instant;
import java.util.Objects;

public record JobRunFailed(JobId jobId, JobRunId runId, String errorType, Instant occurredAt) implements JobDomainEvent {
    public JobRunFailed {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        if (errorType == null || errorType.isBlank()) {
            throw new IllegalArgumentException("errorType cannot be null or blank");
        }
    }
}
