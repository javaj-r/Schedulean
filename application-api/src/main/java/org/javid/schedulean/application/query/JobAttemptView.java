package org.javid.schedulean.application.query;

import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.enums.AttemptStatus;

import java.time.Instant;
import java.util.Objects;

public record JobAttemptView(JobRunId runId,
                             int attempt,
                             AttemptStatus status,
                             Instant startedAt,
                             Instant finishedAt,
                             Long durationMs,
                             String errorType) {

    public JobAttemptView {
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        if (attempt < 1) throw new IllegalArgumentException("attempt must be >= 1");
    }
}
