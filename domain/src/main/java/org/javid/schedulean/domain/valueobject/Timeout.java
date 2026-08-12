package org.javid.schedulean.domain.valueobject;

import java.time.Duration;
import java.util.Objects;

public record Timeout(Duration value) {
    public Timeout {
        Objects.requireNonNull(value, "Timeout duration cannot be null");
        if (value.isNegative()) throw new IllegalArgumentException("Timeout duration cannot be negative");
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
