package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import org.javid.schedulean.application.command.ResumeJobCommand;
import org.javid.schedulean.application.port.in.ResumeJobUseCase;
import org.javid.schedulean.application.port.out.CronNextRunProvider;
import org.javid.schedulean.application.port.out.DomainEventPublisher;
import org.javid.schedulean.application.port.out.JobDefinitionRepository;
import org.javid.schedulean.domain.model.JobDefinition;

import java.time.Instant;

@RequiredArgsConstructor
public class ResumeJobService implements ResumeJobUseCase {

    private final CronNextRunProvider cronNextRunProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final JobDefinitionRepository jobDefinitionRepository;

    @Override
    public void execute(ResumeJobCommand cmd) {
        JobDefinition def = jobDefinitionRepository.findById(cmd.jobId()).orElseThrow();
        def.resume(cmd.occurredAt());

        // Atomically calculate and assign next run before saving
        Instant nextRun = calculateNextRun(def, cmd.occurredAt());
        def.scheduleNextRun(nextRun, cmd.occurredAt());

        jobDefinitionRepository.save(def);
        def.pullEvents().forEach(domainEventPublisher::publish);
    }

    private Instant calculateNextRun(JobDefinition def, Instant occurredAt) {
        var schedule = def.executionConfig().schedule();
        if (schedule.isCron()) {
            return cronNextRunProvider.calculateNextRun(schedule.cron(), occurredAt);
        } else {
            return occurredAt.plus(schedule.intervalValue());
        }
    }
}
