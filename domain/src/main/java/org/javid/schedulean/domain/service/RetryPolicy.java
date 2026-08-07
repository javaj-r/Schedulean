package org.javid.schedulean.domain.service;

import org.javid.schedulean.domain.valueobject.RetrySpec;

import java.time.Duration;
import java.util.Set;

public final class RetryPolicy {

    public boolean shouldRetry(RetrySpec spec, int attemptsSoFar, Throwable t, Set<String> retryOn, Set<String> doNotRetryOn) {
        if (attemptsSoFar >= spec.maxAttempts()) return false;
        if (matchesAny(doNotRetryOn, t)) return false;
        if (retryOn == null || retryOn.isEmpty()) return true;
        return matchesAny(retryOn, t);
    }

    public Duration backoff(RetrySpec spec, int nextAttempt) {
        return spec.delayForAttempt(nextAttempt);
    }

    private boolean matchesAny(Set<String> classNames, Throwable t) {
        if (classNames == null) return false;
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (classNames.contains(c.getClass().getName())) return true;
        }
        return false;
    }
}
