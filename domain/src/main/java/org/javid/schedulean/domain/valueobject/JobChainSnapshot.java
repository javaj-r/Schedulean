package org.javid.schedulean.domain.valueobject;

import org.javid.schedulean.domain.model.JobChainStep;
import org.javid.schedulean.domain.valueobject.enums.ChainStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record JobChainSnapshot(
        ChainId id,
        String name,
        ChainStatus status,
        int currentStep,
        List<JobChainStep> steps,
        Instant createdAt,
        Instant completedAt) {
    public JobChainSnapshot {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(steps, "steps cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");

        if (name.isBlank()) throw new IllegalArgumentException("name cannot be blank");
        if (steps.isEmpty()) throw new IllegalArgumentException("A JobChain must contain at least one step");

        // Defensive copy of mutable list
        steps = List.copyOf(steps);
    }
}