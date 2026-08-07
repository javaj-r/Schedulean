package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.TraceContext;

import java.util.Objects;

public record RunContext(JobRunId runId, JobId jobId, int attempt, TraceContext traceContext) {
    public RunContext {
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(traceContext, "traceContext cannot be null");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }
    }

    public static RunContext of(JobRun run, int attempt, TraceContext tc) {
        return new RunContext(run.id(), run.jobId(), attempt, tc);
    }
}