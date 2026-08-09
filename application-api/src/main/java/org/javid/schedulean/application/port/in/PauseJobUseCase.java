package org.javid.schedulean.application.port.in;

import org.javid.schedulean.application.command.PauseJobCommand;

public interface PauseJobUseCase {
    void execute(PauseJobCommand command);
}
