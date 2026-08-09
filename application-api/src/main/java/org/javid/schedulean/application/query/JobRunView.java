package org.javid.schedulean.application.query;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.TraceId;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

import java.time.Instant;
import java.util.Objects;

public record JobRunView(JobRunId runId,
                         JobId jobId,
                         RunStatus status,
                         Instant startedAt,
                         Instant finishedAt,
                         Long durationMs,
                         String errorType,
                         int attemptCount,
                         TraceId traceId) {

    public JobRunView {
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        // startedAt, finishedAt, durationMs, errorType, traceId can be null depending on state
        if (attemptCount < 0) throw new IllegalArgumentException("attemptCount must be >= 0");
    }
}
