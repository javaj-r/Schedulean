package org.javid.schedulean.application.service;

import java.time.Duration;
import java.time.Instant;

/**
 * Policy for determining when a run is considered stale or a zombie.
 * Ensures heartbeat frequency and recovery timeout cannot drift apart.
 */
public class RecoveryPolicy {

    private final Duration staleAfter;
    private final Duration zombieAfter;

    public RecoveryPolicy(Duration staleAfter, Duration zombieAfter) {
        this.staleAfter = staleAfter;
        this.zombieAfter = zombieAfter;
    }

    public Duration staleAfter() {
        return staleAfter;
    }

    public boolean isZombie(Instant lastHeartbeat) {
        if (lastHeartbeat == null) return false;
        return lastHeartbeat.isBefore(Instant.now().minus(zombieAfter));
    }
}
