package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.valueobject.CronExpression;

import java.time.Instant;

/**
 * Application port to calculate the next cron occurrence.
 * The domain layer holds the CronExpression data, but the actual calculation
 * requires infrastructure libraries (e.g., Spring's CronExpression) which are
 * abstracted behind this port to maintain domain purity.
 */
public interface CronNextRunProvider {
    Instant calculateNextRun(CronExpression cron, Instant referenceTime);
}
