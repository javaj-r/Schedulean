package org.javid.schedulean.application.port.in;


import org.javid.schedulean.application.command.ResumeJobCommand;

public interface ResumeJobUseCase {
    void execute(ResumeJobCommand command);
}
