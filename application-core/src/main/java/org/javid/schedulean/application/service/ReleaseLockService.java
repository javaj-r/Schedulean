package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javid.schedulean.application.command.ReleaseLockCommand;
import org.javid.schedulean.application.port.in.ReleaseLockUseCase;
import org.javid.schedulean.application.port.out.DomainEventPublisher;
import org.javid.schedulean.application.port.out.LockRepository;
import org.javid.schedulean.domain.event.LockReleased;

@Slf4j
@RequiredArgsConstructor
public class ReleaseLockService implements ReleaseLockUseCase {

    private final LockRepository lockRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public void execute(ReleaseLockCommand command) {
        // 1. Force release infrastructure lock
        lockRepository.forceRelease(command.lockName());

        // 2. Publish domain event directly (Locks are infrastructure, not an aggregate state)
        eventPublisher.publish(new LockReleased(command.jobId(), command.lockName(), command.occurredAt()));
        log.info("Forcefully released lock {} for job {}", command.lockName().value(), command.jobId().value());
    }
}
