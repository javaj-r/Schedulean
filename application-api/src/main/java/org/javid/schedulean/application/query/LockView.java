package org.javid.schedulean.application.query;

import org.javid.schedulean.domain.valueobject.LockName;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;

import java.time.Instant;
import java.util.Objects;

public record LockView(LockName name, NodeInstanceId lockedBy, Instant lockedAt, Instant lockUntil) {
    public LockView {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(lockedBy, "lockedBy cannot be null");
        Objects.requireNonNull(lockedAt, "lockedAt cannot be null");
        Objects.requireNonNull(lockUntil, "lockUntil cannot be null");

        // Cross-field invariant: lockUntil must be >= lockedAt
        if (lockUntil.isBefore(lockedAt)) {
            throw new IllegalArgumentException("lockUntil must be greater than or equal to lockedAt");
        }
    }
}