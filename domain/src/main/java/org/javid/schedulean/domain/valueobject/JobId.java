package org.javid.schedulean.domain.valueobject;

public record JobId(String value) {
    public JobId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("id must not be blank");
    }
}
