package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javid.schedulean.application.port.out.*;
import org.javid.schedulean.domain.JobHandler;
import org.javid.schedulean.domain.exception.JobExecutionException;
import org.javid.schedulean.domain.exception.JobTimeoutException;
import org.javid.schedulean.domain.model.JobAttempt;
import org.javid.schedulean.domain.model.JobDefinition;
import org.javid.schedulean.domain.model.JobRun;
import org.javid.schedulean.domain.model.RunContext;
import org.javid.schedulean.domain.service.RetryPolicy;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;
import org.javid.schedulean.domain.valueobject.RetrySpec;
import org.javid.schedulean.domain.valueobject.Timeout;
import org.javid.schedulean.domain.valueobject.TraceContext;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor
public class ExecuteJobService {

    private final NodeRegistryPort nodeRegistry;
    private final JobRunRepository runRepository;
    private final JobHandlerRegistry handlerRegistry;
    private final ObservabilityPort observabilityPort;
    private final JobHeartbeatService heartbeatService;
    private final ExecutorService virtualThreadExecutor;
    private final JobAttemptRepository attemptRepository;
    private final ActiveExecutionRegistry activeExecutionRegistry;

    private final RetryPolicy retryPolicy = new RetryPolicy();
    private final Map<String, Semaphore> perJobLimit = new ConcurrentHashMap<>();

    public void execute(JobDefinition definition, JobRun run) {
        executeLocked(definition, run);
    }

    private void executeLocked(JobDefinition definition, JobRun run) {
        RetrySpec retrySpec = definition.executionConfig().retrySpec();
        Timeout runTimeout = definition.executionConfig().timeout();
        Throwable lastError = null;
        boolean success = false;
        boolean ownershipLost = false;

        NodeInstanceId nodeId = nodeRegistry.nodeId();

        // Must transition to RUNNING before starting attempts
        run.markStarted(nodeId, Instant.now());

        // Use explicit startIfPending method
        if (!runRepository.startIfPending(run, nodeId, run.pullEvents())) {
            log.warn("Failed to start run {}. Ownership lost or already running.", run.id().value());
            return;
        }

        // Heartbeat spans the entire run lifecycle (across all attempts and backoffs)
        Thread executionThread = Thread.currentThread();
        JobHeartbeatService.HeartbeatHandle heartbeatHandle = heartbeatService.start(
                run.id(), Duration.ofSeconds(10), executionThread, activeExecutionRegistry
        );

        try {
            for (int attemptNumber = 1; attemptNumber <= retrySpec.maxAttempts(); attemptNumber++) {
                AttemptResult result = runAttempt(definition, run, attemptNumber, runTimeout, nodeId);
                if (result.success()) {
                    success = true;
                    break;
                }
                // If ownership is lost (interrupted/cancelled), abort immediately. Do not save.
                if (result.ownershipLost()) {
                    ownershipLost = true;
                    break;
                }

                lastError = result.error();

                // If the run itself timed out, stop retrying immediately
                if (lastError instanceof JobTimeoutException && run.status() == RunStatus.TIMED_OUT) {
                    break;
                }

                if (attemptNumber < retrySpec.maxAttempts()) {
                    Duration backoff = retryPolicy.backoff(retrySpec, attemptNumber + 1);
                    try {
                        Thread.sleep(backoff.toMillis());
                    } catch (InterruptedException e) {
                        // Interrupted during backoff (likely heartbeat rejection/ownership loss)
                        Thread.currentThread().interrupt();
                        ownershipLost = true;
                        break;
                    }
                }
            }

            if (!ownershipLost) {
                finalizeRun(definition, run, success, lastError, nodeId);
            } else {
                log.warn("Run {} lost ownership during execution. Aborting without saving aggregate state.", run.id().value());
            }
        } finally {
            heartbeatHandle.stop();
        }
    }

    private AttemptResult runAttempt(JobDefinition definition, JobRun run, int attemptNumber, Timeout runTimeout, NodeInstanceId nodeId) {
        JobAttempt domainAttempt = run.startAttempt(Instant.now());
        attemptRepository.saveIfRunOwnedAndRunning(run.id(), nodeId, domainAttempt);

        Map<String, String> attributes = Map.of(
                "job.id", definition.id().value(),
                "job.attempt", String.valueOf(attemptNumber),
                "job.node", nodeId.value()
        );
        ObservabilityPort.SpanHandle spanHandle = observabilityPort.startSpan("job." + definition.id().value(), attributes);
        ObservabilityPort.TimerSample timerSample = observabilityPort.startTimer();

        Semaphore limiter = perJobLimit.computeIfAbsent(definition.id().value(),
                k -> new Semaphore(definition.executionConfig().maxConcurrent().value()));

        boolean permitAcquired = false;
        AttemptResult result = null;

        try (spanHandle) {
            if (!limiter.tryAcquire()) {
                JobTimeoutException timeoutException = new JobTimeoutException("Local concurrency limit reached");
                run.markAttemptFailed(domainAttempt, timeoutException, Instant.now());
                attemptRepository.saveIfRunOwnedAndRunning(run.id(), nodeId, domainAttempt);
                return new AttemptResult(false, timeoutException, false);
            }
            // Only set to true AFTER successful acquire
            permitAcquired = true;

            JobHandler handler = handlerRegistry.get(definition.jobHandlerKey());
            Future<Object> future = virtualThreadExecutor.submit(() -> handler.execute(RunContext.of(run, attemptNumber, TraceContext.empty())));
            activeExecutionRegistry.register(run.id(), future);

            try {
                if (!runTimeout.isZero()) {
                    Duration remainingTime = calculateRemainingTimeout(domainAttempt.startedAt(), runTimeout);
                    if (remainingTime.isZero() || remainingTime.isNegative()) {
                        throw new JobTimeoutException("Attempt exceeded timeout before execution started");
                    }
                    future.get(remainingTime.toMillis(), TimeUnit.MILLISECONDS);
                    run.markAttemptSucceeded(domainAttempt, Instant.now());
                    attemptRepository.saveIfRunOwnedAndRunning(run.id(), nodeId, domainAttempt);
                    result = new AttemptResult(true, null, false);
                } else {
                    future.get();
                    run.markAttemptSucceeded(domainAttempt, Instant.now());
                    attemptRepository.saveIfRunOwnedAndRunning(run.id(), nodeId, domainAttempt);
                    result = new AttemptResult(true, null, false);
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                run.markAttemptTimedOut(domainAttempt, Instant.now());
                attemptRepository.saveIfRunOwnedAndRunning(run.id(), nodeId, domainAttempt);
                run.timeout(Instant.now());
                // Fenced save for run timeout
                if (!runRepository.saveIfOwnedAndRunning(run, nodeId, run.pullEvents())) {
                    result = new AttemptResult(false, new JobExecutionException("Ownership lost during timeout save"), true);
                } else {
                    spanHandle.recordException(new JobTimeoutException("Execution exceeded remaining run timeout"));
                    spanHandle.markError();
                    result = new AttemptResult(false, new JobTimeoutException("Execution exceeded remaining run timeout"), false);
                }
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                run.markAttemptFailed(domainAttempt, cause, Instant.now());
                attemptRepository.saveIfRunOwnedAndRunning(run.id(), nodeId, domainAttempt);
                spanHandle.recordException(cause);
                spanHandle.markError();
                result = new AttemptResult(false, cause, false);
            } catch (CancellationException | InterruptedException e) {
                Thread.currentThread().interrupt();
                result = new AttemptResult(false, new JobExecutionException("Execution cancelled/interrupted", e), true);
            } finally {
                activeExecutionRegistry.unregister(run.id());
            }

        } catch (Throwable throwable) {
            // Check for interruption before saving to prevent overwriting recovery
            if (Thread.currentThread().isInterrupted()) {
                result = new AttemptResult(false, new JobExecutionException("Interrupted", throwable), true);
            } else {
                run.markAttemptFailed(domainAttempt, throwable, Instant.now());
                attemptRepository.saveIfRunOwnedAndRunning(run.id(), nodeId, domainAttempt);
                spanHandle.recordException(throwable);
                spanHandle.markError();
                result = new AttemptResult(false, throwable, false);
            }
        } finally {
            // Only release if we actually acquired
            if (permitAcquired) {
                limiter.release();
            }
            observabilityPort.recordTimer(timerSample, "job.attempt.duration",
                    "job", definition.id().value(), "attempt", String.valueOf(attemptNumber));
        }
        return result;
    }

    private Duration calculateRemainingTimeout(Instant attemptStartedAt, Timeout runTimeout) {
        Instant deadline = attemptStartedAt.plus(runTimeout.value());
        Duration remaining = Duration.between(Instant.now(), deadline);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private void finalizeRun(JobDefinition definition, JobRun run, boolean success, Throwable lastError, NodeInstanceId nodeId) {
        // If run was already marked TIMED_OUT in an attempt, don't overwrite it
        if (run.status() != RunStatus.TIMED_OUT) {
            if (success) {
                run.succeed(null, Instant.now());
            } else {
                run.fail(lastError != null ? lastError : new JobExecutionException("unknown"), Instant.now());
            }
        }

        // Fenced save with atomic outbox event persistence
        boolean saved = runRepository.saveIfOwnedAndRunning(run, nodeId, run.pullEvents());
        if (!saved) {
            log.warn("Failed to finalize run {}. Ownership lost or already reclaimed.", run.id().value());
        }
        observabilityPort.incrementCounter(success ? "job.success" : "job.failed", "job", definition.id().value());
    }

    private record AttemptResult(boolean success, Throwable error, boolean ownershipLost) {
    }
}