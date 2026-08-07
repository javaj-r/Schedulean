package org.javid.schedulean.domain.exception;

/**
 * Thrown when the database is unavailable and the circuit breaker is open.
 */
public class DatabaseUnavailableException extends RuntimeException {
    public DatabaseUnavailableException(Throwable cause) {
        super(cause);
    }
}