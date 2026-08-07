package org.javid.schedulean.domain.valueobject;

import org.javid.schedulean.domain.valueobject.enums.RetryMode;

import java.time.Duration;

public record RetrySpec(int maxAttempts, RetryMode mode, Duration delay, double multiplier, Duration maxDelay) {
    public RetrySpec {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        if (mode == null) mode = RetryMode.FIXED;
        if (delay == null || delay.isNegative()) throw new IllegalArgumentException("delay must be >= 0");
        if (multiplier < 1.0) throw new IllegalArgumentException("multiplier must be >= 1.0");
        if (maxDelay == null || maxDelay.isNegative()) throw new IllegalArgumentException("maxDelay must be >= 0");
        if (maxDelay.compareTo(delay) < 0) throw new IllegalArgumentException("maxDelay cannot be less than delay");
    }

    public Duration delayForAttempt(int attempt) {
        if (mode == RetryMode.FIXED) return delay;
        long ms = (long) (delay.toMillis() * Math.pow(multiplier, attempt - 1D));
        return Duration.ofMillis(Math.min(ms, maxDelay.toMillis()));
    }

    public static RetrySpec disabled() {
        return new RetrySpec(1, RetryMode.FIXED, Duration.ZERO, 1.0, Duration.ZERO);
    }

    public static RetrySpec of(int max, RetryMode mode, Duration delay, double mult, Duration maxDelay) {
        return new RetrySpec(max, mode, delay, mult, maxDelay);
    }
}
