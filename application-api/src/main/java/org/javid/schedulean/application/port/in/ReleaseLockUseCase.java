package org.javid.schedulean.application.port.in;

import org.javid.schedulean.application.command.ReleaseLockCommand;

public interface ReleaseLockUseCase {
    void execute(ReleaseLockCommand command);
}
