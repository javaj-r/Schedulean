package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record TraceId(String value) {
    public TraceId {
        Objects.requireNonNull(value, "TraceId cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException("TraceId cannot be blank");
    }
}