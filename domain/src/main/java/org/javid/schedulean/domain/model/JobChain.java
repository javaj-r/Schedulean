package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.event.ChainAdvanced;
import org.javid.schedulean.domain.event.ChainCompleted;
import org.javid.schedulean.domain.event.ChainFailed;
import org.javid.schedulean.domain.event.DomainEvent;
import org.javid.schedulean.domain.exception.ChainExecutionException;
import org.javid.schedulean.domain.valueobject.ChainId;
import org.javid.schedulean.domain.valueobject.JobChainSnapshot;
import org.javid.schedulean.domain.valueobject.enums.ChainStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JobChain {

    public static final String OCCURRED_AT_CANNOT_BE_NULL = "occurredAt cannot be null";
    private final ChainId id;
    private final String name;
    private ChainStatus status;
    private int currentStep;
    private final List<JobChainStep> steps;
    private final Instant createdAt;
    private Instant completedAt;

    private final List<DomainEvent> events = new ArrayList<>();

    /**
     * Primary package-private constructor for NEW chains.
     */
    JobChain(ChainId id, String name, List<JobChainStep> steps, Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) throw new ChainExecutionException("name cannot be blank");
        Objects.requireNonNull(steps, "steps cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);

        if (steps.isEmpty()) {
            throw new ChainExecutionException("A JobChain must contain at least one step");
        }

        // Validate sequence is strictly 1...n in order
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).sequence() != i + 1) {
                throw new ChainExecutionException("Steps must be sequentially ordered starting from 1");
            }
        }

        this.steps = new ArrayList<>(steps);
        this.createdAt = occurredAt;
        this.currentStep = 1;
        this.status = ChainStatus.RUNNING;
    }

    /**
     * Package-private constructor for RECONSTITUTING chains from persistence.
     */
    JobChain(JobChainSnapshot snapshot) {
        this.id = snapshot.id();
        this.name = snapshot.name();
        this.createdAt = snapshot.createdAt();
        this.steps = new ArrayList<>(snapshot.steps()); // snapshot already returns unmodifiable copy

        // Override default state with historical state
        this.status = snapshot.status();
        this.currentStep = snapshot.currentStep();
        this.completedAt = snapshot.completedAt();
    }

    public enum StepOutcome {SUCCESS, FAILURE, TIMED_OUT, CANCELLED, SKIPPED}

    /**
     * Records the outcome of the current step and atomically advances the chain state.
     * This encapsulates the policy and state change to prevent bypassing.
     * Structural validity is guaranteed by the constructor, so no trigger mode
     * validation is needed here.
     */
    public void recordCurrentStepOutcome(StepOutcome outcome, Instant occurredAt) {
        Objects.requireNonNull(outcome, "outcome cannot be null");
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);

        if (status != ChainStatus.RUNNING) {
            throw new ChainExecutionException("Cannot record outcome for a chain that is not RUNNING. Current state: " + status);
        }

        boolean isFailure = (outcome == StepOutcome.FAILURE || outcome == StepOutcome.TIMED_OUT || outcome == StepOutcome.CANCELLED);

        // If we are at the last step, the chain completes or fails based on the outcome.
        if (currentStep >= steps.size()) {
            if (isFailure) {
                fail(occurredAt);
            } else {
                complete(occurredAt);
            }
            return;
        }

        boolean canAdvance = canAdvance(outcome);

        if (canAdvance) {
            currentStep++;
            events.add(new ChainAdvanced(id, currentStep, occurredAt));
        } else {
            fail(occurredAt);
        }
    }

    /**
     * Evaluate if we can advance to the next step based on the next step's trigger mode
     *
     * @param outcome StepOutcome
     * @return boolean
     */
    private boolean canAdvance(StepOutcome outcome) {
        JobChainStep nextStep = steps.get(currentStep); // 0-based index, so currentStep points to the next step
        return switch (nextStep.triggerMode()) {
            case ON_PREVIOUS_SUCCESS -> outcome == StepOutcome.SUCCESS;
            // ON_PREVIOUS_COMPLETION advances if the step didn't fail/timed out/cancelled (e.g., SUCCESS or SKIPPED)
            case ON_PREVIOUS_NON_FAILURE -> outcome == StepOutcome.SUCCESS || outcome == StepOutcome.SKIPPED;
            // ALWAYS advances regardless of the outcome (even on failure)
            case ALWAYS -> true;
            // START is impossible here because the constructor prevented it for sequence > 1
            case START ->
                    throw new ChainExecutionException("Structural violation: START trigger mode on non-first step");
        };
    }

    public void fail(Instant occurredAt) {
        Objects.requireNonNull(occurredAt, OCCURRED_AT_CANNOT_BE_NULL);
        if (status != ChainStatus.RUNNING) {
            throw new ChainExecutionException("Cannot fail a chain that is not RUNNING"); // Idempotent
        }
        this.status = ChainStatus.FAILED;
        this.completedAt = occurredAt;
        events.add(new ChainFailed(id, occurredAt));
    }

    private void complete(Instant occurredAt) {
        if (status != ChainStatus.RUNNING) {
            throw new ChainExecutionException("Cannot complete a chain that is not RUNNING"); // Consistent transition rules
        }
        this.status = ChainStatus.COMPLETED;
        this.completedAt = occurredAt;
        events.add(new ChainCompleted(id, occurredAt));
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(events);
        events.clear();
        return copy;
    }

    public JobChainStep currentStepDetails() {
        return steps.get(currentStep - 1);
    }

    public ChainId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public ChainStatus status() {
        return status;
    }

    public int currentStep() {
        return currentStep;
    }

    public List<JobChainStep> steps() {
        return Collections.unmodifiableList(steps);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant completedAt() {
        return completedAt;
    }
}
