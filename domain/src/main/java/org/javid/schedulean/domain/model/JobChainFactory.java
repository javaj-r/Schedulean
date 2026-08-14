package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.valueobject.ChainId;
import org.javid.schedulean.domain.valueobject.JobChainSnapshot;

import java.time.Instant;
import java.util.List;

/**
 * Factory class for creating and reconstituting JobChain aggregates.
 * Acts as the only public entry point for obtaining JobChain instances.
 */
public final class JobChainFactory {

    private JobChainFactory() {
        /* This utility class should not be instantiated */
    }

    /**
     * Creates a NEW JobChain in its initial PENDING state.
     */
    public static JobChain createNewChain(ChainId id, String name, List<JobChainStep> steps, Instant occurredAt) {
        return new JobChain(id, name, steps, occurredAt);
    }

    /**
     * Reconstitutes a JobChain from a persisted snapshot.
     * Delegates to the package-private static method inside JobChain to ensure encapsulation.
     */
    public static JobChain reconstitute(JobChainSnapshot snapshot) {
        return new JobChain(snapshot);
    }
}