package org.javid.schedulean.application.command;

import org.javid.schedulean.domain.valueobject.JobExecutionConfig;
import org.javid.schedulean.domain.valueobject.JobHandlerKey;
import org.javid.schedulean.domain.valueobject.JobId;

import java.time.Instant;
import java.util.Objects;

public record ScheduleJobCommand(JobId jobId,
                                 String displayName,
                                 JobHandlerKey jobHandlerKey,
                                 JobExecutionConfig executionConfig,
                                 Instant occurredAt) {

    public ScheduleJobCommand {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(jobHandlerKey, "jobHandlerKey cannot be null");
        Objects.requireNonNull(executionConfig, "executionConfig cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be null or blank");
        }
    }
}
