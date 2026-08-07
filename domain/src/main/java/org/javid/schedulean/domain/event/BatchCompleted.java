package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobBatchId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event indicating that a job batch has successfully completed all its jobs.
 */
public record BatchCompleted(JobBatchId batchId, Instant occurredAt) implements DomainEvent {
    public BatchCompleted {
        Objects.requireNonNull(batchId, "batchId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}