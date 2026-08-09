package org.javid.schedulean.application.port.out;

import java.util.Map;

public interface ObservabilityPort {
    SpanHandle startSpan(String name, Map<String, String> attributes);

    TimerSample startTimer();

    void incrementCounter(String name, String... tags);

    void recordTimer(TimerSample sample, String name, String... tags);

    interface SpanHandle extends AutoCloseable {
        void setAttribute(String key, String value);

        void setAttribute(String key, long value);

        void recordException(Throwable t);

        void markError();

        String spanId();

        @Override
        void close();
    }

    interface TimerSample {
    }

    TimerSample NO_TIMER = new TimerSample() {
    };
}
