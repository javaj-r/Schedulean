package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.valueobject.JobDefinitionSnapshot;
import org.javid.schedulean.domain.valueobject.JobExecutionConfig;
import org.javid.schedulean.domain.valueobject.JobHandlerKey;
import org.javid.schedulean.domain.valueobject.JobId;

import java.time.Instant;

/**
 * Factory class for creating and reconstituting JobDefinition aggregates.
 * Acts as the only public entry point for obtaining JobDefinition instances.
 */
public final class JobDefinitionFactory {

    private JobDefinitionFactory() {
        /* This utility class should not be instantiated */
    }

    /**
     * Creates a NEW JobDefinition in its initial PENDING state.
     */
    public static JobDefinition createNewDefinition(
            JobId id,
            String displayName,
            JobHandlerKey jobHandlerKey,
            JobExecutionConfig executionConfig,
            Instant occurredAt) {

        return new JobDefinition(id, displayName, jobHandlerKey, executionConfig, occurredAt);
    }

    /**
     * Reconstitutes a JobDefinition from a persisted snapshot.
     * Delegates to the package-private static method inside JobDefinition to ensure encapsulation.
     */
    public static JobDefinition reconstitute(JobDefinitionSnapshot snapshot) {
        return new JobDefinition(snapshot);
    }
}