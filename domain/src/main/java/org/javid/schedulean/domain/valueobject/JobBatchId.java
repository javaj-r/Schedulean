package org.javid.schedulean.domain.valueobject;

public record JobBatchId(Long value) {
    public JobBatchId {
        if (value == null || value < 1) throw new IllegalArgumentException("JobBatchId must be positive");
    }
}