package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.valueobject.JobRunId;

import java.util.concurrent.Future;

/**
 * Tracks the lifecycle of active job runs (the orchestrator threads).
 * Used by ExecuteJobService to register/unregister runs, and by GracefulShutdownService
 * to wait for completion or force cancellation.
 */
public interface ActiveRunRegistry {

    /**
     * Registers an active run orchestrator.
     * Returns a handle that allows the orchestrator to update its current handler future.
     */
    RunExecutionHandle register(JobRunId runId, Thread orchestratorThread);

    /**
     * Unregisters a completed run orchestrator.
     */
    void unregister(JobRunId runId);

    /**
     * Returns the count of currently active run orchestrators.
     */
    int activeCount();

    /**
     * Interrupts all currently active orchestrator threads AND cancels their handler futures.
     */
    void cancelAll();

    /**
     * A handle for a specific run execution, allowing the orchestrator to update
     * the active handler future so it can be cancelled during shutdown.
     */
    interface RunExecutionHandle {
        /**
         * Sets or clears the current handler future for this run.
         * Pass null to clear the future when an attempt completes and before backoff.
         */
        void setHandlerFuture(Future<?> future);
    }
}