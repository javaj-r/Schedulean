package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.valueobject.*;

import java.time.Instant;

/**
 * Factory class for creating and reconstituting JobRun aggregates.
 * Acts as the only public entry point for obtaining JobRun instances.
 */
public final class JobRunFactory {

    private JobRunFactory() {
        /* This utility class should not be instantiated */
    }

    /**
     * Creates a NEW JobRun in its initial PENDING state.
     */
    public static JobRun createNewRun(
            JobRunId id,
            JobId jobId,
            NodeInstanceId createdByNodeId,
            Instant scheduledAt,
            TraceContext traceContext,
            ChainId chainId,
            JobBatchId batchId) {

        return new JobRun(id, jobId, createdByNodeId, scheduledAt, traceContext, chainId, batchId);
    }

    /**
     * Reconstitutes a JobRun from a persisted snapshot.
     * Delegates to the package-private static method inside JobRun to ensure encapsulation.
     */
    public static JobRun reconstitute(JobRunSnapshot snapshot) {
        return new JobRun(snapshot);
    }
}