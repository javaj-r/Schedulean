package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.model.JobRun;
import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobRunRepository {
    JobRun save(JobRun run);

    Optional<JobRun> findById(JobRunId id);

    List<JobRun> findByJobIdOrderByStartedAtDesc(JobId jobId, int limit);

    List<JobRun> findByStatusAndStartedAtAfter(RunStatus runStatus, Instant since);

    Optional<JobRun> findTopByJobIdOrderByStartedAtDesc(JobId jobId);

    void updateHeartbeat(JobRunId runId, NodeInstanceId nodeId, Instant heartbeat);

    List<JobRunId> findOrphanedRuns(Instant staleBefore);

    void reclaimRun(JobRunId runId, String reason);

    void markRunningRunsAsOrphaned(NodeInstanceId nodeId);
}
