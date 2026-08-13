package org.javid.schedulean.domain;

import org.javid.schedulean.domain.model.RunContext;

/**
 * Port (Interface) to be implemented by other bounded contexts or infrastructure adapters
 * to define the actual business logic of a job.
 * <br>
 * CONTRACT: Implementations MUST be cooperative with interruption.
 * When a job exceeds its timeout, the virtual thread executing this method will be interrupted.
 * Implementations MUST:
 * - Use interruptible I/O operations (e.g., InputStream, Channel, HttpClient)
 * - Check Thread.currentThread().isInterrupted() periodically in long-running loops
 * - Propagate InterruptedException by rethrowing it or restoring the interrupt flag
 * Failure to handle interruption may result in the handler continuing to execute
 * after the JobRun has been marked TIMED_OUT.
 */
public interface JobHandler {
    /**
     * Executes the job logic.
     *
     * @param runContext immutable snapshot of the run + attempt + parameters
     * @return optional result payload (stored when storeResult=true on JobDefinition)
     * @throws InterruptedException if the thread was interrupted during execution
     */
    Object execute(RunContext runContext) throws InterruptedException;
}