package org.javid.schedulean.domain.valueobject;

public record Priority(int value) {
    public Priority {
        if (value < 1 || value > 999) throw new IllegalArgumentException("priority 1-999");
    }
}
