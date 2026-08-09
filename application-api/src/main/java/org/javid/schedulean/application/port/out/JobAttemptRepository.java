package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.model.JobAttempt;
import org.javid.schedulean.domain.valueobject.JobRunId;

public interface JobAttemptRepository {
    void save(JobRunId runId, JobAttempt attempt);
}
