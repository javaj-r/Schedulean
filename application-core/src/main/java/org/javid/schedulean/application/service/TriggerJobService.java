package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javid.schedulean.application.command.TriggerJobCommand;
import org.javid.schedulean.application.port.in.TriggerJobUseCase;
import org.javid.schedulean.application.port.out.*;
import org.javid.schedulean.domain.model.JobDefinition;
import org.javid.schedulean.domain.model.JobInvocation;
import org.javid.schedulean.domain.model.JobRun;
import org.javid.schedulean.domain.model.JobRunFactory;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.SpanId;
import org.javid.schedulean.domain.valueobject.TraceContext;
import org.javid.schedulean.domain.valueobject.TraceId;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class TriggerJobService implements TriggerJobUseCase {

    private final JobDefinitionRepository definitionRepository;
    private final JobRunRepository runRepository;
    private final JobDispatchPort dispatchPort;
    private final NodeRegistryPort registry;
    private final IdGenerationPort idGenerator;

    @Override
    public JobRunId execute(TriggerJobCommand command) {
        JobDefinition definition = definitionRepository.findById(command.jobId())
                .orElseThrow(() -> new IllegalArgumentException("JobDefinition not found: " + command.jobId().value()));

        // Generate ID via the port
        JobRunId runId = new JobRunId(idGenerator.generate());

        // Use command.occurredAt() instead of Instant.now()
        JobRun run = JobRunFactory.createNewRun(
                runId, command.jobId(), registry.nodeId(), command.occurredAt(),
                TraceContext.empty(), definition.chainId(), command.batchId()
        );

        // Check result of creation. Do not dispatch if creation fails.
        boolean created = runRepository.create(run, run.pullEvents());
        if (!created) {
            log.error("Failed to create run {}. Aborting dispatch.", runId.value());
            throw new IllegalStateException("Failed to create job run: " + runId.value());
        }

        JobInvocation invocation = new JobInvocation(
                command.jobId(), definition.jobHandlerKey(), null, Map.of(),
                run.id(), registry.nodeId(),
                new TraceId("empty"), new SpanId("empty"),
                command.occurredAt()
        );
        dispatchPort.dispatch(invocation);

        log.info("Triggered job {} with run ID {}", command.jobId().value(), runId.value());
        return run.id();
    }
}
