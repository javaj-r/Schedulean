package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;
import org.javid.schedulean.domain.valueobject.enums.RecoveryReason;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

import java.time.Instant;
import java.util.List;

/**
 * Port for finding and reclaiming orphaned/zombie job runs.
 */
public interface OrphanedJobRecoveryPort {

    /**
     * Finds runs that are stale and need recovery.
     * <br>
     * The adapter query MUST select:
     * 1. RUNNING runs WHERE last_heartbeat < staleBefore
     * 2. PENDING runs WHERE scheduled_at < staleBefore (abandoned dispatches)
     * 3. RECOVERING runs WHERE last_heartbeat < staleBefore (stranded recovery processes)
     */
    List<RecoveryCandidate> findRecoveryCandidates(Instant staleBefore);

    /**
     * Marks all RUNNING runs for a specific node as immediately recoverable.
     * Called during graceful shutdown.
     * Implementations MUST update last_heartbeat to an old timestamp (e.g., epoch)
     * so that findRecoveryCandidates picks them up instantly.
     * <p>
     * FIX: Adapter MUST fence this operation:
     * WHERE status = 'RUNNING' AND executing_node_id = ?
     */
    void markRunningRunsAsOrphaned(NodeInstanceId nodeId);

    /**
     * A typed record representing a stale run, its current persisted status, and the calculated reason.
     */
    record RecoveryCandidate(JobRunId runId, RunStatus currentStatus, RecoveryReason reason) {
        public RecoveryCandidate {
            java.util.Objects.requireNonNull(runId, "runId cannot be null");
            java.util.Objects.requireNonNull(currentStatus, "currentStatus cannot be null");
            java.util.Objects.requireNonNull(reason, "reason cannot be null");
        }
    }
}
