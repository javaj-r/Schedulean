package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record JobHandlerKey(String value) {

    public JobHandlerKey {
        Objects.requireNonNull(value, "JobHandlerKey cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("JobHandlerKey cannot be blank");
        }
    }
}