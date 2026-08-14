package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.JobHandler;
import org.javid.schedulean.domain.valueobject.JobHandlerKey;

public interface JobHandlerRegistry {
    /**
     * Retrieves the job handler for the given key.
     */
    JobHandler get(JobHandlerKey jobHandlerKey);

    /**
     * Shuts down all active job handlers.
     * Implementations MUST cancel any running threads/futures and release resources
     * to prevent duplicate execution during a graceful shutdown.
     */
    void shutdown();
}
