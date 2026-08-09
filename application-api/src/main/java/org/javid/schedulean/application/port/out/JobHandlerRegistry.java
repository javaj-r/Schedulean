package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.JobHandler;
import org.javid.schedulean.domain.valueobject.JobHandlerKey;

public interface JobHandlerRegistry {
    JobHandler get(JobHandlerKey jobHandlerKey);
}
