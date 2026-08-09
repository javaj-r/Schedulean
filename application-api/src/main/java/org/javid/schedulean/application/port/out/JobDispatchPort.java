package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.model.JobInvocation;
import org.javid.schedulean.domain.valueobject.JobBatchId;

import java.util.List;
import java.util.Objects;

public interface JobDispatchPort {
    void dispatch(JobInvocation invocation);

    BatchResult dispatchAtomically(List<JobInvocation> invocations);

    record BatchResult(boolean success, int dispatchedCount, JobBatchId batchId, String error) {
        public BatchResult {
            Objects.requireNonNull(batchId, "batchId cannot be null"); // Even if null conceptually, we use a Null Object or require it. For success, it's generated.
        }
    }
}
