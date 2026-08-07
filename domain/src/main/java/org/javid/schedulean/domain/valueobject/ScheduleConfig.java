package org.javid.schedulean.domain.valueobject;

import java.time.Duration;

public record ScheduleConfig(CronExpression cron, Interval interval) {
    public ScheduleConfig {
        if (cron == null && interval == null) {
            throw new IllegalArgumentException("Either cron or interval must be provided");
        }
        if (cron != null && interval != null) {
            throw new IllegalArgumentException("Only one of cron or interval can be set, not both");
        }
    }

    public static ScheduleConfig cron(CronExpression c) {
        return new ScheduleConfig(c, null);
    }

    public static ScheduleConfig interval(Duration d) {
        return new ScheduleConfig(null, new Interval(d));
    }

    public boolean isCron() {
        return cron != null;
    }

    public Duration intervalValue() {
        return interval != null ? interval.value() : null;
    }
}
