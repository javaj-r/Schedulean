package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.valueobject.JobRunId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface OrphanedJobRecoveryPort {
    List<JobRunId> findOrphanedRuns(Instant staleBefore);

    void reclaimRun(JobRunId runId, String reason);

    List<JobRunId> findZombieRuns(Duration heartbeatTimeout);
}
