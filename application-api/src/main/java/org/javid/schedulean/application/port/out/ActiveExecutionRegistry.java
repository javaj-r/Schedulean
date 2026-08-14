package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.valueobject.JobRunId;

import java.util.concurrent.Future;

public interface ActiveExecutionRegistry {
    void register(JobRunId runId, Future<?> future);

    void unregister(JobRunId runId);

    int activeCount();

    void cancelAll();

    /**
     * Cancels the execution future for a specific run.
     * Used by the heartbeat service when ownership is lost to interrupt the handler thread.
     */
    void cancel(JobRunId runId);
}
