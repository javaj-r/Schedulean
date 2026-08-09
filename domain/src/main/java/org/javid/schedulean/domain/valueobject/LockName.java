package org.javid.schedulean.domain.valueobject;

import org.javid.schedulean.domain.exception.InvalidJobDefinitionException;

import java.util.Objects;

public record LockName(String value) {
    public LockName {
        Objects.requireNonNull(value, "LockName cannot be null");
        if (value.isBlank()) {
            throw new InvalidJobDefinitionException("LockName cannot be blank");
        }
    }
}
