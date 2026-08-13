package org.javid.schedulean.domain.valueobject;

import org.javid.schedulean.domain.valueobject.enums.JobState;
import java.time.Instant;
import java.util.Objects;

public record JobDefinitionSnapshot(
        JobId id,
        String displayName,
        JobHandlerKey jobHandlerKey,
        JobExecutionConfig executionConfig,
        JobState state,
        boolean storeResult,
        ChainId chainId,
        int chainSequence,
        Instant nextRunAt) {
    public JobDefinitionSnapshot {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(jobHandlerKey, "jobHandlerKey cannot be null");
        Objects.requireNonNull(executionConfig, "executionConfig cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
    }
}