package org.javid.schedulean.application.port.in;

import org.javid.schedulean.application.command.UpdateJobDefinitionCommand;

public interface UpdateJobDefinitionUseCase {
    void execute(UpdateJobDefinitionCommand command);
}
