package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import org.javid.schedulean.application.command.PauseJobCommand;
import org.javid.schedulean.application.port.in.PauseJobUseCase;
import org.javid.schedulean.application.port.out.JobDefinitionRepository;
import org.javid.schedulean.domain.model.JobDefinition;

@RequiredArgsConstructor
public class PauseJobService implements PauseJobUseCase {

    private final JobDefinitionRepository repo;

    @Override
    public void execute(PauseJobCommand cmd) {
        JobDefinition def = repo.findById(cmd.jobId()).orElseThrow();
        def.pause(cmd.occurredAt());

        // Pass events to repository for transactional outbox persistence
        repo.save(def, def.pullEvents());
    }
}
