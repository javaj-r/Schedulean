package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record ChainId(String value) {
    public ChainId {
        Objects.requireNonNull(value, "ChainId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ChainId cannot be blank");
        }
    }
}
