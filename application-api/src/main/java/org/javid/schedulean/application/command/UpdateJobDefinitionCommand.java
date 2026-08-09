package org.javid.schedulean.application.command;

import org.javid.schedulean.domain.valueobject.JobExecutionConfig;
import org.javid.schedulean.domain.valueobject.JobId;

import java.time.Instant;
import java.util.Objects;

public record UpdateJobDefinitionCommand(JobId jobId, JobExecutionConfig executionConfig, Instant occurredAt) {
    public UpdateJobDefinitionCommand {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(executionConfig, "executionConfig cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
