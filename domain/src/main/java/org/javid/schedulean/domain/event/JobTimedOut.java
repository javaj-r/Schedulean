package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;

import java.time.Instant;
import java.util.Objects;

public record JobTimedOut(JobId jobId, JobRunId runId, Instant occurredAt) implements JobDomainEvent {
    public JobTimedOut {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
