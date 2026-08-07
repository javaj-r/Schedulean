package org.javid.schedulean.domain.valueobject;

import java.time.Duration;

public record Timeout(Duration value) {
    public Timeout {
        if (value == null) {
            value = Duration.ZERO; // default to zero if null provided
        }
        if (value.isNegative()) {
            throw new IllegalArgumentException("Timeout duration cannot be negative");
        }
    }

    public static Timeout of(Duration d) {
        return new Timeout(d);
    }

    public static Timeout zero() {
        return new Timeout(Duration.ZERO);
    }

    public boolean isZero() {
        return value.isZero();
    }
}
