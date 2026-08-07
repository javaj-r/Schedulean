package org.javid.schedulean.domain.valueobject;

import java.time.Duration;

public record Interval(Duration value) {

    public Interval {
        if (value == null || value.isNegative() || value.isZero())
            throw new IllegalArgumentException("interval must be positive");
    }
}
