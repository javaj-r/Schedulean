package org.javid.schedulean.domain.model;

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
                              String externalRef) {

    public JobFailureEvent {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        if (attemptCount < 1) {
            throw new IllegalArgumentException("attemptCount must be >= 1");
        }
        errorType = Objects.requireNonNullElse(errorType, "Unknown");
        errorMessage = Objects.requireNonNullElse(errorMessage, "No error message provided");
        externalRef = Objects.requireNonNullElse(externalRef, "");
    }

    public enum Severity {LOW, MEDIUM, HIGH, CRITICAL}
}
