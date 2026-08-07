package org.javid.schedulean.domain.valueobject;

import java.time.Duration;

public record LockConfig(LockName name, Duration lockAtMostFor, Duration lockAtLeastFor) {
    public LockConfig {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        if (lockAtMostFor == null || lockAtMostFor.isNegative() || lockAtMostFor.isZero())
            throw new IllegalArgumentException("lockAtMostFor must be positive");
        if (lockAtLeastFor == null || lockAtLeastFor.isNegative())
            throw new IllegalArgumentException("lockAtLeastFor must be >= 0");
        if (lockAtLeastFor.compareTo(lockAtMostFor) > 0)
            throw new IllegalArgumentException("lockAtLeastFor must be <= lockAtMostFor");
    }
}
