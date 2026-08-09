package org.javid.schedulean.application.command;

import org.javid.schedulean.domain.valueobject.JobBatchId;
import org.javid.schedulean.domain.valueobject.JobId;

import java.time.Instant;
import java.util.Objects;

public record TriggerJobCommand(JobId jobId, JobBatchId batchId, Instant occurredAt) {

    public TriggerJobCommand {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        // batchId is optional (nullable)
    }

    public TriggerJobCommand(JobId jobId, Instant occurredAt) {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        this(jobId, null, occurredAt);
    }
}
