package org.javid.schedulean.application.query;

import org.javid.schedulean.domain.valueobject.JobId;

import java.util.Objects;

public record QueryJobRunsQuery(JobId jobId, int limit) {
    public QueryJobRunsQuery {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        if (limit < 1) throw new IllegalArgumentException("limit must be >= 1");
    }
}
