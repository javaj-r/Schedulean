package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.valueobject.JobId;
import org.javid.schedulean.domain.valueobject.JobRunId;

public interface NodeNotificationPort {
    void notifyJobReady(JobId jobId, JobRunId runId);
}
