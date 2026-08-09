package org.javid.schedulean.application.command;

import org.javid.schedulean.domain.valueobject.JobId;
import java.time.Instant;
import java.util.Objects;

public record PauseJobCommand(JobId jobId, Instant occurredAt) {
    public PauseJobCommand {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
