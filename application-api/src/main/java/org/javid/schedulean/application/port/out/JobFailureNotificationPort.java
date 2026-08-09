package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.model.JobFailureEvent;

public interface JobFailureNotificationPort {
    void notify(JobFailureEvent event);
}
