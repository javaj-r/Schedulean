package org.javid.schedulean.application.port.in;

import org.javid.schedulean.application.query.*;
import org.javid.schedulean.domain.valueobject.JobRunId;

import java.util.List;

public interface QueryJobHistoryUseCase {
    List<JobRunView> findJobRuns(QueryJobRunsQuery query);

    List<JobAttemptView> findJobAttempts(JobRunId runId);

    List<JobRunView> findFailedRuns(QueryFailedRunsQuery query);

    List<LockView> findAllLocks();
}
