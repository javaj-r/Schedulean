package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.event.DomainEvent;
import org.javid.schedulean.domain.model.JobRun;
import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobRunRepository {

    /**
     * Atomically creates a new run (PENDING) and its events.
     */
    boolean create(JobRun run, List<DomainEvent> events);

    /**
     * Atomically transitions a run from PENDING to RUNNING.
     * Adapter MUST fence: WHERE status = 'PENDING'
     */
    boolean startIfPending(JobRun run, NodeInstanceId nodeId, List<DomainEvent> events);

    /**
     * Atomically saves a run that is already RUNNING and owned by the expected node.
     * Adapter MUST fence: WHERE status = 'RUNNING' AND executing_node_id = ?
     */
    boolean saveIfOwnedAndRunning(JobRun run, NodeInstanceId expectedNodeId, List<DomainEvent> events);

    /**
     * FIX: Atomically transitions a run from RUNNING/PENDING to RECOVERING.
     * Adapter MUST fence: WHERE id = ? AND status IN ('RUNNING', 'PENDING') AND last_heartbeat < ?
     * This prevents stale workers from overwriting recovery claims.
     */
    boolean transitionToRecovering(JobRun run, NodeInstanceId recoveringNodeId, Instant staleBefore, List<DomainEvent> events);

    /**
     * FIX: Atomically transitions a run from RECOVERING to FAILED (or terminal).
     * Adapter MUST fence: WHERE status = 'RECOVERING' AND executing_node_id = ?
     */
    boolean saveIfRecoveringAndOwned(JobRun run, NodeInstanceId expectedNodeId, List<DomainEvent> events);

    /**
     * Finds runs by job ID, ordered by startedAt descending.
     */
    List<JobRun> findByJobIdOrderByStartedAtDesc(JobId jobId, int limit);

    /**
     * Finds runs by status and startedAt after a given instant.
     */
    List<JobRun> findByStatusAndStartedAtAfter(RunStatus status, Instant since);

    /**
     * Finds the top run by job ID, ordered by startedAt descending.
     */
    Optional<JobRun> findTopByJobIdOrderByStartedAtDesc(JobId jobId);

    /**
     * Finds a run by its ID.
     */
    Optional<JobRun> findById(JobRunId id);
}
