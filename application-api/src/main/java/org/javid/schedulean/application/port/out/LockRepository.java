package org.javid.schedulean.application.port.out;

import org.javid.schedulean.application.query.LockView;
import org.javid.schedulean.domain.valueobject.LockName;

import java.time.Duration;
import java.util.List;

public interface LockRepository {

    /**
     * Executes a task under a distributed lock.
     */
    void executeWithLock(LockName lockName, Duration lockAtMostFor, Duration lockAtLeastFor, Runnable task);

    /**
     * Forcefully releases a lock in the infrastructure (e.g., updates the DB row to set lock_until = now()).
     * <p>
     * NOTE: This method performs infrastructure action ONLY. It does NOT generate domain events.
     * The calling application service MUST invoke the aggregate's releaseLock() method to record
     * the LockReleased domain event consistently.
     * </p>
     */
    void forceRelease(LockName lockName);

    /**
     * Extends the duration of the currently held lock.
     */
    void extendActiveLock(Duration additional);

    /**
     * Finds all currently held locks (where lock_until > current timestamp).
     * Released or expired locks are excluded.
     *
     * @return List of LockView representing active locks.
     */
    List<LockView> findAll();
}
