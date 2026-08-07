package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.LockName;

import java.time.Instant;
import java.util.Objects;

public record LockReleased(JobId jobId, LockName lockName, Instant occurredAt) implements JobDomainEvent {
    public LockReleased {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(lockName, "lockName cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}