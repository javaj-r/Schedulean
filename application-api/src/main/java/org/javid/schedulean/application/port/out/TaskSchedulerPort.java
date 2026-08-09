package org.javid.schedulean.application.port.out;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

public interface TaskSchedulerPort {
    ScheduledFuture<?> scheduleWithCron(Runnable task, String cron, String zoneId);

    ScheduledFuture<?> scheduleWithFixedRate(Runnable task, Duration rate);

    void executeNow(Runnable task);

    void cancel(ScheduledFuture<?> future);
}
