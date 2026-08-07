package org.javid.schedulean.domain.valueobject;

public record JobRunId(Long value) {
    public JobRunId {
        if (value == null || value < 1) throw new IllegalArgumentException("JobRunId must be positive");
    }
}