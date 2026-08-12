package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record JobId(String value) {
    public JobId {
        Objects.requireNonNull(value, "JobId cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException("JobId cannot be blank");
    }
}
