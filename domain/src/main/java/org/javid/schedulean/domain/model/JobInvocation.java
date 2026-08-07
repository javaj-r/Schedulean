package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.valueobject.JobHandlerKey;
import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record JobInvocation(JobId jobId,
                            JobHandlerKey jobHandlerKey,
                            String methodName,
                            Map<String, Object> args,
                            JobRunId runId,
                            String traceId,
                            String spanId,
                            Instant enqueuedAt) {

    public JobInvocation {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(jobHandlerKey, "jobHandlerKey cannot be null");
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(enqueuedAt, "enqueuedAt cannot be null");

        methodName = Objects.requireNonNullElse(methodName, "");
        traceId = Objects.requireNonNullElse(traceId, "");
        spanId = Objects.requireNonNullElse(spanId, "");

        // Defensive copy of mutable Map
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}