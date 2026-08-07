package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;

import java.time.Instant;
import java.util.Objects;

public record JobRunStarted(JobId jobId, JobRunId runId, NodeInstanceId executingNodeId, Instant occurredAt) implements JobDomainEvent {
    public JobRunStarted {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(runId, "runId cannot be null");
        Objects.requireNonNull(executingNodeId, "executingNodeId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }
}
