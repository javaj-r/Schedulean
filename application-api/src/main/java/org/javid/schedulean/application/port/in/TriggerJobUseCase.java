package org.javid.schedulean.application.port.in;

import org.javid.schedulean.application.command.TriggerJobCommand;
import org.javid.schedulean.domain.valueobject.JobRunId;

public interface TriggerJobUseCase {
    JobRunId execute(TriggerJobCommand command);
}
