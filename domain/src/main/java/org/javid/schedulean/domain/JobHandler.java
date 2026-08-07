package org.javid.schedulean.domain;

import org.javid.schedulean.domain.model.RunContext;

/**
 * Port (Interface) to be implemented by other bounded contexts or infrastructure adapters
 * to define the actual business logic of a job.
 */
public interface JobHandler {
    /**
     * Executes the job logic.
     *
     * @param runContext immutable snapshot of the run + attempt + parameters
     * @return optional result payload (stored when storeResult=true on JobDefinition)
     */
    Object execute(RunContext runContext);
}