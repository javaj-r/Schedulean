package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

public record TraceContext(TraceId traceId, SpanId spanId) {
    public TraceContext {
        Objects.requireNonNull(traceId, "traceId cannot be null");
        Objects.requireNonNull(spanId, "spanId cannot be null");
    }

    public static TraceContext empty() {
        return new TraceContext(new TraceId("empty"), new SpanId("empty"));
    }
}