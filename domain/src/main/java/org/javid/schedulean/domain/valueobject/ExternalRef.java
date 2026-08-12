package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record ExternalRef(String value) {
    public ExternalRef {
        Objects.requireNonNull(value, "ExternalRef cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException("ExternalRef cannot be blank");
    }
}