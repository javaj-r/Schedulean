package org.javid.schedulean.domain.valueobject;

import org.javid.schedulean.domain.valueobject.enums.BatchStatus;

import java.time.Instant;
import java.util.Objects;

public record JobBatchSnapshot(
        JobBatchId id,
        String name,
        BatchStatus status,
        int totalJobs,
        int succeeded,
        int failed,
        Instant createdAt,
        Instant completedAt) {
    public JobBatchSnapshot {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");

        if (name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
        if (totalJobs < 1) throw new IllegalArgumentException("totalJobs must be >= 1");
        if (succeeded < 0 || failed < 0) throw new IllegalArgumentException("succeeded/failed cannot be negative");
    }
}