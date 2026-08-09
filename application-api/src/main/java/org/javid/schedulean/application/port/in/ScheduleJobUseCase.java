package org.javid.schedulean.application.port.in;

import org.javid.schedulean.application.command.ScheduleJobCommand;

public interface ScheduleJobUseCase {
    void execute(ScheduleJobCommand command);
}
