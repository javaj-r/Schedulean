package org.javid.schedulean.domain.exception;

/**
 * Thrown when a distributed lock cannot be acquired or is forcefully lost.
 */
public class LockAcquisitionException extends RuntimeException {
    public LockAcquisitionException(String msg) {
        super(msg);
    }
}