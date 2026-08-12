package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record JobRunId(String value) {
    public JobRunId {
        Objects.requireNonNull(value, "JobRunId cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException("JobRunId cannot be blank");
    }
}
