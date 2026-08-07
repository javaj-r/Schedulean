package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.exception.JobExecutionException;
import org.javid.schedulean.domain.valueobject.enums.AttemptStatus;

import java.time.Instant;
import java.util.Objects;

public class JobAttempt {

    private final int number;
    private final Instant startedAt;
    private Instant finishedAt;
    private AttemptStatus status;
    private boolean lockAcquired;
    private String errorType;
    private String errorMessage;

    JobAttempt(int number, Instant startedAt) {
        if (number < 1) throw new JobExecutionException("attempt number must be >= 1");
        this.number = number;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt cannot be null");
        this.status = AttemptStatus.RUNNING;
    }

    void succeed(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        if (this.status != AttemptStatus.RUNNING) {
            throw new JobExecutionException("Cannot succeed an attempt that is not RUNNING");
        }
        this.status = AttemptStatus.SUCCESS;
        this.finishedAt = occurredAt;
    }

    void fail(Throwable t, Instant occurredAt) {
        Objects.requireNonNull(t, "throwable cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        if (this.status != AttemptStatus.RUNNING) {
            throw new JobExecutionException("Cannot fail an attempt that is not RUNNING");
        }
        this.status = AttemptStatus.FAILED;
        this.finishedAt = occurredAt;
        this.errorType = t.getClass().getName();
        this.errorMessage = t.getMessage();
    }

    void markLockAcquired(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        if (this.status != AttemptStatus.RUNNING) {
            throw new JobExecutionException("Cannot mark lock acquired for an attempt that is not RUNNING");
        }
        this.lockAcquired = true;
        // If we want an event for this, we'd add it to the parent JobRun's event list
        // via a return value or callback, but since JobAttempt is an entity,
        // it typically doesn't hold its own event list. The JobRun aggregate root
        // will emit the JobAttemptStarted event when startAttempt() is called.
    }

    public int number() {
        return number;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public AttemptStatus status() {
        return status;
    }

    public boolean lockAcquired() {
        return lockAcquired;
    }

    public String errorType() {
        return errorType;
    }

    public String errorMessage() {
        return errorMessage;
    }
}
