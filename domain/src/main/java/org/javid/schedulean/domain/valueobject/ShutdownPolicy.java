package org.javid.schedulean.domain.valueobject;

import java.time.Duration;
import java.util.Objects;

/**
 * Domain Value Object representing the configuration for a graceful shutdown.
 */
public record ShutdownPolicy(
        Duration drainTimeout,
        Duration pollInterval,
        Duration postCancelGracePeriod) {
    public ShutdownPolicy {
        Objects.requireNonNull(drainTimeout, "drainTimeout cannot be null");
        Objects.requireNonNull(pollInterval, "pollInterval cannot be null");
        Objects.requireNonNull(postCancelGracePeriod, "postCancelGracePeriod cannot be null");

        if (drainTimeout.isNegative() || drainTimeout.isZero()) {
            throw new IllegalArgumentException("drainTimeout must be positive");
        }
        if (pollInterval.isNegative() || pollInterval.isZero()) {
            throw new IllegalArgumentException("pollInterval must be positive");
        }
        if (postCancelGracePeriod.isNegative()) {
            throw new IllegalArgumentException("postCancelGracePeriod cannot be negative");
        }
    }
}