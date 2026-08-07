package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobBatchId;
import java.time.Instant;
import java.util.Objects;

public record BatchUpdated(JobBatchId batchId, boolean success, Instant occurredAt) implements DomainEvent {
    public BatchUpdated {
        Objects.requireNonNull(batchId, "batchId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}