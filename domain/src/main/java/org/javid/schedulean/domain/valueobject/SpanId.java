package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record SpanId(String value) {
    public SpanId {
        Objects.requireNonNull(value, "SpanId cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException("SpanId cannot be blank");
    }
}
