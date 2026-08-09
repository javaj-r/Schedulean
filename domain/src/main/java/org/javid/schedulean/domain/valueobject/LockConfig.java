package org.javid.schedulean.domain.valueobject;

import org.javid.schedulean.domain.exception.InvalidJobDefinitionException;

import java.time.Duration;
import java.util.Objects;

public record LockConfig(LockName name, Duration lockAtMostFor, Duration lockAtLeastFor) {
    public LockConfig {
        Objects.requireNonNull(name, "LockConfig name cannot be null");
        Objects.requireNonNull(lockAtMostFor, "lockAtMostFor cannot be null");
        Objects.requireNonNull(lockAtLeastFor, "lockAtLeastFor cannot be null");

        if (lockAtMostFor.isNegative() || lockAtMostFor.isZero()) {
            throw new InvalidJobDefinitionException("lockAtMostFor must be positive");
        }
        if (lockAtLeastFor.isNegative()) {
            throw new InvalidJobDefinitionException("lockAtLeastFor must be >= 0");
        }
        if (lockAtLeastFor.compareTo(lockAtMostFor) > 0) {
            throw new InvalidJobDefinitionException("lockAtLeastFor must be <= lockAtMostFor");
        }
    }
}
