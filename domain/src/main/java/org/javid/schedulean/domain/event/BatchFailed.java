package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobBatchId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event indicating that a job batch has failed (at least one job in the batch failed).
 */
public record BatchFailed(JobBatchId batchId, Instant occurredAt) implements DomainEvent {
    public BatchFailed {
        Objects.requireNonNull(batchId, "batchId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}