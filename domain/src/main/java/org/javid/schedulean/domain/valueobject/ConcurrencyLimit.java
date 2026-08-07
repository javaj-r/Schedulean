package org.javid.schedulean.domain.valueobject;

public record ConcurrencyLimit(int value) {
    public ConcurrencyLimit {
        if (value < 1) throw new IllegalArgumentException("concurrency >= 1");
    }
}
