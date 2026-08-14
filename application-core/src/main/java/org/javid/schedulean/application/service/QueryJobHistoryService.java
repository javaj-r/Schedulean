package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import org.javid.schedulean.application.port.in.QueryJobHistoryUseCase;
import org.javid.schedulean.application.port.out.JobAttemptRepository;
import org.javid.schedulean.application.port.out.JobRunRepository;
import org.javid.schedulean.application.port.out.LockRepository;
import org.javid.schedulean.application.query.*;
import org.javid.schedulean.domain.model.JobAttempt;
import org.javid.schedulean.domain.model.JobRun;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

import java.util.List;

@RequiredArgsConstructor
public class QueryJobHistoryService implements QueryJobHistoryUseCase {

    private final LockRepository lockRepository;
    private final JobRunRepository jobRunRepository;
    private final JobAttemptRepository jobAttemptRepository;

    @Override
    public List<JobRunView> findJobRuns(QueryJobRunsQuery query) {
        return jobRunRepository.findByJobIdOrderByStartedAtDesc(query.jobId(), query.limit())
                .stream()
                .map(this::mapToJobRunView)
                .toList();
    }

    @Override
    public List<JobAttemptView> findJobAttempts(JobRunId runId) {
        return jobAttemptRepository.findByRunIdOrderByAttemptAsc(runId)
                .stream()
                .map(attempt -> mapToJobAttemptView(runId, attempt))
                .toList();
    }

    @Override
    public List<JobRunView> findFailedRuns(QueryFailedRunsQuery query) {
        return jobRunRepository.findByStatusAndStartedAtAfter(RunStatus.FAILED, query.since())
                .stream()
                .map(this::mapToJobRunView)
                .toList();
    }

    @Override
    public List<LockView> findAllLocks() {
        return lockRepository.findAll();
    }

    private JobRunView mapToJobRunView(JobRun run) {
        return new JobRunView(
                run.id(), run.jobId(), run.status(),
                run.startedAt(), run.finishedAt(), run.durationMs(),
                run.errorType(), run.attemptCount(), run.traceContext().traceId()
        );
    }

    private JobAttemptView mapToJobAttemptView(JobRunId runId, JobAttempt attempt) {
        return new JobAttemptView(
                runId,
                attempt.number(),
                attempt.status(),
                attempt.startedAt(),
                attempt.finishedAt(),
                attempt.finishedAt() != null && attempt.startedAt() != null ? attempt.finishedAt().toEpochMilli() - attempt.startedAt().toEpochMilli() : 0,
                attempt.errorType()
        );
    }
}
