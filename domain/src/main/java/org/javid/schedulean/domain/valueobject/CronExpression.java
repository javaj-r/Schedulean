package org.javid.schedulean.domain.valueobject;

import java.time.ZoneId;

public record CronExpression(String value, ZoneId timezone) {
    public CronExpression {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("cron must not be blank");
        if (timezone == null) throw new IllegalArgumentException("timezone must not be null");
    }
}
