package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record JobBatchId(String value) {
    public JobBatchId {
        Objects.requireNonNull(value, "JobBatchId cannot be null");
        if (value.isBlank()) throw new IllegalArgumentException("JobBatchId cannot be blank");
    }
}
