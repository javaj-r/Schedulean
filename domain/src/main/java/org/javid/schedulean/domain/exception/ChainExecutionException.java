package org.javid.schedulean.domain.exception;

/**
 * Thrown when a JobChain attempts an invalid state transition or violates step rules.
 */
public class ChainExecutionException extends RuntimeException {
    public ChainExecutionException(String msg) {
        super(msg);
    }
}