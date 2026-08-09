package org.javid.schedulean.application.command;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.LockName;

import java.time.Instant;
import java.util.Objects;

public record ReleaseLockCommand(JobId jobId, LockName lockName, Instant occurredAt) {
    public ReleaseLockCommand {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(lockName, "lockName cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
