package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record NodeInstanceId(String value) {
    public NodeInstanceId {
        Objects.requireNonNull(value, "NodeInstanceId cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException("NodeInstanceId cannot be blank");
    }
}