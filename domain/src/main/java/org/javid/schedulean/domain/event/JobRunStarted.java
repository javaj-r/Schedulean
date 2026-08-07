package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;

import java.time.Instant;
import java.util.Objects;

public record JobRunStarted(JobId jobId, JobRunId runId, String executingNodeId, Instant occurredAt) implements JobDomainEvent {
    public JobRunStarted {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        if (executingNodeId == null || executingNodeId.isBlank()) {
            throw new IllegalArgumentException("executingNodeId cannot be null or blank");
        }
    }
}
