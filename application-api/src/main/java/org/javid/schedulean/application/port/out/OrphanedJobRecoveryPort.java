package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;
import org.javid.schedulean.domain.valueobject.enums.RecoveryReason;

import java.time.Instant;
import java.util.List;

/**
 * Port for finding and reclaiming orphaned/zombie job runs.
 */
public interface OrphanedJobRecoveryPort {

    /**
     * Finds runs that are stale (heartbeat older than threshold) and still RUNNING or PENDING.
     */
    List<RecoveryCandidate> findRecoveryCandidates(Instant staleBefore);

    /**
     * Marks all RUNNING runs for a specific node as orphaned.
     * Called during graceful shutdown to immediately flag runs
     * for recovery rather than waiting for the stale timeout.
     */
    void markRunningRunsAsOrphaned(NodeInstanceId nodeId);

    /**
     * A typed record representing a stale run and the calculated reason.
     */
    record RecoveryCandidate(JobRunId runId, RecoveryReason reason) {
        public RecoveryCandidate {
            java.util.Objects.requireNonNull(runId, "runId cannot be null");
            java.util.Objects.requireNonNull(reason, "reason cannot be null");
        }
    }
}
