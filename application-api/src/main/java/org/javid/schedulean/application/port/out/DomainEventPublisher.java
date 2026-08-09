package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
