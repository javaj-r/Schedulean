package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;

import java.time.Instant;
import java.util.Objects;

public record JobNextRunCleared(JobId jobId, Instant occurredAt) implements JobDomainEvent {
    public JobNextRunCleared {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
