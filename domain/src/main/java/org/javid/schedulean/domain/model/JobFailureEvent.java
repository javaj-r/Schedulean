package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.valueobject.ExternalRef;
import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;

import java.time.Instant;
import java.util.Objects;

public record JobFailureEvent(JobId jobId,
                              JobRunId runId,
                              int attemptCount,
                              String errorType,
                              String errorMessage,
                              Severity severity,
                              Instant occurredAt,
                              ExternalRef externalRef) {

    public JobFailureEvent {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        Objects.requireNonNull(externalRef, "externalRef cannot be null");

        if (attemptCount < 1)
            throw new IllegalArgumentException("attemptCount must be >= 1");
        if (errorType == null || errorType.isBlank())
            throw new IllegalArgumentException("errorType cannot be null or blank");
        if (errorMessage == null || errorMessage.isBlank())
            throw new IllegalArgumentException("errorMessage cannot be null or blank");
    }

    public enum Severity {LOW, MEDIUM, HIGH, CRITICAL}
}
