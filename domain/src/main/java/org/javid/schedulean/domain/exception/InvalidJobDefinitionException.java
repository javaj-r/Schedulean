package org.javid.schedulean.domain.exception;

/**
 * Thrown when a JobDefinition is created or reconfigured with invalid parameters.
 */
public class InvalidJobDefinitionException extends RuntimeException {
    public InvalidJobDefinitionException(String msg) {
        super(msg);
    }
}