package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.event.*;
import org.javid.schedulean.domain.exception.InvalidJobDefinitionException;
import org.javid.schedulean.domain.exception.JobExecutionException;
import org.javid.schedulean.domain.valueobject.*;
import org.javid.schedulean.domain.valueobject.enums.JobState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JobDefinition {

    public static final String OCCURRED_AT_CANNOT_BE_NULL = "occurredAt cannot be null";
    private final JobId id;
    private final String displayName;
    private final JobHandlerKey jobHandlerKey;

    private JobExecutionConfig executionConfig;

    private JobState state;
    private boolean storeResult;

    private ChainId chainId;
    private int chainSequence;

    private Instant nextRunAt;

    private final List<DomainEvent> events = new ArrayList<>();

    public JobDefinition(JobId id,
                         String displayName,
                         JobHandlerKey jobHandlerKey,
                         JobExecutionConfig executionConfig,
                         Instant occurredAt) {

        this.id = Objects.requireNonNull(id, "JobId cannot be null");
        this.displayName = requireNonBlank(displayName, "Display name cannot be blank");
        this.jobHandlerKey = Objects.requireNonNull(jobHandlerKey, "JobHandlerKey cannot be null");
        this.executionConfig = Objects.requireNonNull(executionConfig, "JobExecutionConfig cannot be null");

        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);

        this.state = JobState.ACTIVE;
        this.storeResult = false;
        this.chainId = null;
        this.chainSequence = 0;
        this.nextRunAt = null;

        this.events.add(new JobScheduled(id, occurredAt));
    }

    public void pause(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);

        if (this.state != JobState.ACTIVE) {
            throw new JobExecutionException("Job is already paused");
        }
        this.state = JobState.PAUSED;
        this.nextRunAt = null; // Clear schedule when paused
        this.events.add(new JobPaused(id, occurredAt));
    }

    public void resume(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.state != JobState.PAUSED) {
            throw new JobExecutionException("Job is already active");
        }
        this.state = JobState.ACTIVE;
        // The application layer must call scheduleNextRun() after resume
        this.events.add(new JobResumed(id, occurredAt));
    }

    public void reconfigure(JobExecutionConfig newConfig, Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);

        this.executionConfig = Objects.requireNonNull(newConfig, "JobExecutionConfig cannot be null");
        this.nextRunAt = null; // Clear old nextRunAt when schedule changes
        this.events.add(new JobDefinitionChanged(id, occurredAt));
    }

    /**
     * Explicitly sets the next due time.
     * Fails if the job is currently paused.
     */
    public void scheduleNextRun(Instant nextRunAt, Instant occurredAt) {
        Objects.requireNonNull(nextRunAt, "nextRunAt cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (nextRunAt.isBefore(occurredAt)) {
            throw new JobExecutionException("nextRunAt cannot be before occurredAt " + occurredAt);
        }
        if (this.state != JobState.ACTIVE) {
            throw new JobExecutionException("Cannot schedule next run for a paused job: " + id.value());
        }
        this.nextRunAt = nextRunAt;
        this.events.add(new JobNextRunScheduled(id, nextRunAt, occurredAt)); // New Event
    }

    public void clearNextRun(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.nextRunAt == null) {
            throw new InvalidJobDefinitionException("Cannot clear next run: no run is currently scheduled.");
        }
        this.nextRunAt = null;
        this.events.add(new JobNextRunCleared(id, occurredAt));
    }

    public void enableResultStorage(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (this.storeResult) {
            throw new InvalidJobDefinitionException("Cannot enable result storage: it is already enabled.");
        }
        this.storeResult = true;
        this.events.add(new JobResultStorageEnabled(id, occurredAt));
    }

    /**
     * Assigns this job to a specific step in a chain.
     */
    public void assignToChain(ChainId chainId, int sequence, Instant occurredAt) {
        Objects.requireNonNull(chainId, "chainId cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (sequence < 1) throw new InvalidJobDefinitionException("Chain sequence must be >= 1");

        if (this.chainId != null) {
            throw new InvalidJobDefinitionException("Cannot assign to chain: job is already assigned to chain " + this.chainId.value());
        }

        this.chainId = chainId;
        this.chainSequence = sequence;
        this.events.add(new JobAssignedToChain(id, chainId, sequence, occurredAt));
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(events);
        events.clear();
        return copy;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new InvalidJobDefinitionException(message);
        return value;
    }

    // Getters
    public JobId id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public JobHandlerKey jobHandlerKey() {
        return jobHandlerKey;
    }

    public JobExecutionConfig executionConfig() {
        return executionConfig;
    }

    // Convenience getters for config
    public ScheduleConfig schedule() {
        return executionConfig.schedule();
    }

    public LockConfig lockConfig() {
        return executionConfig.lockConfig();
    }

    public RetrySpec retrySpec() {
        return executionConfig.retrySpec();
    }

    public Priority priority() {
        return executionConfig.priority();
    }

    public ServerTags serverTags() {
        return executionConfig.serverTags();
    }

    public Timeout timeout() {
        return executionConfig.timeout();
    }

    public ConcurrencyLimit maxConcurrent() {
        return executionConfig.maxConcurrent();
    }

    public JobState state() {
        return state;
    }

    public boolean isActive() {
        return state == JobState.ACTIVE;
    }

    public boolean storeResult() {
        return storeResult;
    }

    public ChainId chainId() {
        return chainId;
    }

    public int chainSequence() {
        return chainSequence;
    }

    public Instant nextRunAt() {
        return nextRunAt;
    }
}
