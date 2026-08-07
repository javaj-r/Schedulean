package org.javid.schedulean.domain.exception;

/**
 * Thrown when a JobRun attempts an invalid state transition.
 */
public class JobExecutionException extends RuntimeException {
    public JobExecutionException(String msg) {
        super(msg);
    }

    public JobExecutionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}