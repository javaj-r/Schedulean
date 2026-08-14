package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;

import java.time.Instant;

/**
 * Port for updating job run state without loading the full aggregate.
 * Used for high-frequency operations like heartbeats.
 */
public interface JobRunUpdatePort {

    /**
     * Updates the heartbeat timestamp for a run.
     * <br>
     * The adapter MUST execute a conditional UPDATE:
     * UPDATE job_run SET last_heartbeat = ?
     * WHERE id = ? AND executing_node_id = ? AND status = 'RUNNING'
     * <br>
     * If recovery has claimed the run (status changed to RECOVERING) or the run finished,
     * this update will affect 0 rows and return false.
     * This allows the original executor to detect it has lost ownership and stop its heartbeat loop.
     *
     * @param runId The ID of the run to update
     * @param nodeId The node sending the heartbeat
     * @param heartbeat The timestamp of the heartbeat
     * @return true if the heartbeat was accepted (run is still RUNNING), false otherwise
     */
    boolean updateHeartbeat(JobRunId runId, NodeInstanceId nodeId, Instant heartbeat);
}
