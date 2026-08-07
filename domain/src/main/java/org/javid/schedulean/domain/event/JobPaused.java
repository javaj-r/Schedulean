package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event indicating that a job definition has been paused.
 */
public record JobPaused(JobId jobId, Instant occurredAt) implements JobDomainEvent {
    public JobPaused {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
