package org.javid.schedulean.domain.exception;

/**
 * Thrown when a JobRun is interrupted because it exceeded its configured timeout.
 */
public class JobTimeoutException extends RuntimeException {
    public JobTimeoutException(String msg) {
        super(msg);
    }
}