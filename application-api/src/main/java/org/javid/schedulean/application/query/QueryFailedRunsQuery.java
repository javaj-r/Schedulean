package org.javid.schedulean.application.query;

import java.time.Instant;
import java.util.Objects;

public record QueryFailedRunsQuery(Instant since) {
    public QueryFailedRunsQuery {
        Objects.requireNonNull(since, "since cannot be null");
    }
}
