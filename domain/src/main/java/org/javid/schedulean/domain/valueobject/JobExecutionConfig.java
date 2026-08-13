package org.javid.schedulean.domain.valueobject;

import java.util.Objects;

/**
 * Groups all execution-related configuration for a JobDefinition.
 * Applies defaults for optional values at creation.
 */
public record JobExecutionConfig(
        ScheduleConfig schedule,
        LockConfig lockConfig,
        RetrySpec retrySpec,
        Priority priority,
        ServerTags serverTags,
        Timeout timeout,
        ConcurrencyLimit maxConcurrent
) {
    public JobExecutionConfig {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(lockConfig, "lockConfig cannot be null");

        // Apply defaults
        retrySpec = Objects.requireNonNullElseGet(retrySpec, RetrySpec::disabled);
        priority = Objects.requireNonNullElseGet(priority, () -> new Priority(100));
        serverTags = Objects.requireNonNullElseGet(serverTags, ServerTags::empty);
        timeout = Objects.requireNonNullElseGet(timeout, Timeout::zero);
        maxConcurrent = Objects.requireNonNullElseGet(maxConcurrent, () -> new ConcurrencyLimit(1));
    }
}