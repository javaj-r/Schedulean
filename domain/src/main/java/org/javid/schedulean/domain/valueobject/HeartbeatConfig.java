package org.javid.schedulean.domain.valueobject;

import java.time.Duration;

public record HeartbeatConfig(Duration interval, Duration timeout) {

    public HeartbeatConfig {
        if (interval == null || interval.isNegative() || interval.isZero())
            throw new IllegalArgumentException("interval must be positive");
        if (timeout == null || timeout.compareTo(interval.multipliedBy(2)) < 0)
            throw new IllegalArgumentException("timeout must be >= 2x interval");
    }

    public static HeartbeatConfig defaultConfig() {
        return new HeartbeatConfig(Duration.ofSeconds(10), Duration.ofMinutes(1));
    }
}
