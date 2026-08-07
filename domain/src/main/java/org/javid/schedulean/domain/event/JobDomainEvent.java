package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;

public interface JobDomainEvent extends DomainEvent {

    JobId jobId();
}
