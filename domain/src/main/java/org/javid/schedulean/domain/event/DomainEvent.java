package org.javid.schedulean.domain.event;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredAt();
}
