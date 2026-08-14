package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.model.JobAttempt;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;

import java.util.List;

public interface JobAttemptRepository {

    /**
     * Fenced save. The adapter MUST only save if the parent run is still RUNNING
     * and owned by the expectedNodeId.
     */
    void saveIfRunOwnedAndRunning(JobRunId runId, NodeInstanceId expectedNodeId, JobAttempt attempt);

    List<JobAttempt> findByRunIdOrderByAttemptAsc(JobRunId runId);
}
