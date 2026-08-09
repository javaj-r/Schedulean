package org.javid.schedulean.domain.valueobject;

import org.javid.schedulean.domain.exception.InvalidJobDefinitionException;

import java.util.Objects;

public record NodeInstanceId(String value) {
    public NodeInstanceId {
        Objects.requireNonNull(value, "NodeInstanceId cannot be null");
        if (value.isBlank()) throw new InvalidJobDefinitionException("NodeInstanceId cannot be blank");
    }
}