package org.javid.schedulean.domain.event;

import org.javid.schedulean.domain.valueobject.ChainId;
import org.javid.schedulean.domain.valueobject.JobId;

import java.time.Instant;
import java.util.Objects;

public record JobAssignedToChain(JobId jobId, ChainId chainId, int sequence,
                                 Instant occurredAt) implements JobDomainEvent {
    public JobAssignedToChain {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(chainId, "chainId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        if (sequence < 1) throw new IllegalArgumentException("Chain sequence must be >= 1");

    }
}
