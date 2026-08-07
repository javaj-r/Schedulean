package org.javid.schedulean.domain.valueobject;

public record LockName(String value) {
    public LockName {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("lock name must not be blank");
    }
}
