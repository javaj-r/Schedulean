package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.ChainId;
import java.time.Instant;
import java.util.Objects;

public record ChainCompleted(ChainId chainId, Instant occurredAt) implements DomainEvent {
    public ChainCompleted {
        Objects.requireNonNull(chainId, "chainId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}