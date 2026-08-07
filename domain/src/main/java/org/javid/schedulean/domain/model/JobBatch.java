package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.event.BatchCompleted;
import org.javid.schedulean.domain.event.BatchFailed;
import org.javid.schedulean.domain.event.BatchUpdated;
import org.javid.schedulean.domain.event.DomainEvent;
import org.javid.schedulean.domain.exception.BatchExecutionException;
import org.javid.schedulean.domain.valueobject.JobBatchId;
import org.javid.schedulean.domain.valueobject.enums.BatchStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JobBatch {
    public static final String OCCURRED_AT_CANNOT_BE_NULL = "occurredAt cannot be null";
    private final JobBatchId id;
    private final String name;
    private BatchStatus status;
    private final int totalJobs;
    private int succeeded;
    private int failed;
    private final Instant createdAt;
    private Instant completedAt;

    private final List<DomainEvent> events = new ArrayList<>();

    public JobBatch(JobBatchId id, String name, int totalJobs, Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = requireNonBlank(name, "name cannot be blank");
        if (totalJobs < 1) throw new BatchExecutionException("totalJobs must be >= 1");
        this.totalJobs = totalJobs;
        this.status = BatchStatus.RUNNING;
        this.createdAt = Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
    }

    public void recordSuccess(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (status != BatchStatus.RUNNING) {
            throw new BatchExecutionException("Cannot record success for a batch that is not RUNNING");
        }
        succeeded++;
        events.add(new BatchUpdated(id, true, occurredAt));
        checkCompletion(occurredAt);
    }

    public void recordFailure(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (status != BatchStatus.RUNNING) {
            throw new BatchExecutionException("Cannot record failure for a batch that is not RUNNING");
        }
        failed++;
        events.add(new BatchUpdated(id, false, occurredAt));
        checkCompletion(occurredAt);
    }

    private void checkCompletion(Instant occurredAt) {
        if (succeeded + failed >= totalJobs) {
            if (failed == 0) {
                this.status = BatchStatus.COMPLETED;
                this.completedAt = occurredAt;
                events.add(new BatchCompleted(id, occurredAt));
            } else {
                this.status = BatchStatus.FAILED;
                this.completedAt = occurredAt;
                events.add(new BatchFailed(id, occurredAt));
            }
        }
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(events);
        events.clear();
        return copy;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new BatchExecutionException(message);
        return value;
    }

    public JobBatchId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public BatchStatus status() {
        return status;
    }

    public int totalJobs() {
        return totalJobs;
    }

    public int succeeded() {
        return succeeded;
    }

    public int failed() {
        return failed;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant completedAt() {
        return completedAt;
    }
}