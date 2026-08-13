package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.valueobject.JobBatchId;
import org.javid.schedulean.domain.valueobject.JobBatchSnapshot;

import java.time.Instant;

/**
 * Factory class for creating and reconstituting JobBatch aggregates.
 * Acts as the only public entry point for obtaining JobBatch instances.
 */
public final class JobBatchFactory {

    /**
     * Creates a NEW JobBatch in its initial PENDING state.
     */
    public JobBatch createNewBatch(JobBatchId id, String name, int totalJobs, Instant occurredAt) {
        return new JobBatch(id, name, totalJobs, occurredAt);
    }

    /**
     * Reconstitutes a JobBatch from a persisted snapshot.
     * Delegates to the package-private static method inside JobBatch to ensure encapsulation.
     */
    public JobBatch reconstitute(JobBatchSnapshot snapshot) {
        return new JobBatch(snapshot);
    }
}