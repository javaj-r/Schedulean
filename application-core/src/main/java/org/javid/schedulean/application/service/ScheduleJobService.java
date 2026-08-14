package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javid.schedulean.application.command.ScheduleJobCommand;
import org.javid.schedulean.application.port.in.ScheduleJobUseCase;
import org.javid.schedulean.application.port.out.CronNextRunProvider;
import org.javid.schedulean.application.port.out.DomainEventPublisher;
import org.javid.schedulean.application.port.out.JobDefinitionRepository;
import org.javid.schedulean.domain.model.JobDefinition;
import org.javid.schedulean.domain.model.JobDefinitionFactory;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class ScheduleJobService implements ScheduleJobUseCase {

    private final JobDefinitionRepository repo;
    private final CronNextRunProvider cronProvider;
    private final DomainEventPublisher events;

    @Override
    public void execute(ScheduleJobCommand command) {

        JobDefinition jobDefinition = JobDefinitionFactory.createNewDefinition(
                command.jobId(), command.displayName(), command.jobHandlerKey(),
                command.executionConfig(), command.occurredAt()
        );

        Instant nextRun = calculateNextRun(jobDefinition, command.occurredAt());
        jobDefinition.scheduleNextRun(nextRun, command.occurredAt());

        // Pass events to repository for transactional outbox persistence
        repo.save(jobDefinition, jobDefinition.pullEvents());
    }

    private Instant calculateNextRun(JobDefinition def, Instant occurredAt) {
        var schedule = def.executionConfig().schedule();
        if (schedule.isCron()) {
            return cronProvider.calculateNextRun(schedule.cron(), occurredAt);
        } else {
            return occurredAt.plus(schedule.intervalValue());
        }
    }
}
