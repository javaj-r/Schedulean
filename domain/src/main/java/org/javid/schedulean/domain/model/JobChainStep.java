package org.javid.schedulean.domain.model;

import org.javid.schedulean.domain.exception.ChainExecutionException;
import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.enums.ChainStepTriggerMode;

import java.util.Objects;

public record JobChainStep(int sequence, JobId jobId, ChainStepTriggerMode triggerMode) {

    public JobChainStep {
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(triggerMode, "triggerMode cannot be null");

        if (sequence < 1) {
            throw new ChainExecutionException("sequence must be >= 1");
        }

        // Explicitly enforce trigger mode rules at construction time
        if (sequence == 1 && triggerMode != ChainStepTriggerMode.START) {
            throw new ChainExecutionException("First step sequence must have START trigger mode");
        }
        if (sequence > 1 && triggerMode == ChainStepTriggerMode.START) {
            throw new ChainExecutionException("Only the first step can have START trigger mode");
        }
    }
}