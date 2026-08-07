package org.javid.schedulean.domain.exception;

/**
 * Thrown when a JobBatch attempts an invalid state transition.
 */
public class BatchExecutionException extends RuntimeException {
    public BatchExecutionException(String msg) {
        super(msg);
    }
}