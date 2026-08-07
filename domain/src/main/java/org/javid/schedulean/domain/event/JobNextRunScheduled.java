package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;

import java.time.Instant;
import java.util.Objects;

public record JobNextRunScheduled(JobId jobId, Instant nextRunAt, Instant occurredAt) implements JobDomainEvent {
    public JobNextRunScheduled {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(nextRunAt, "nextRunAt cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
