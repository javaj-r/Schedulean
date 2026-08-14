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
    private final ActiveRunRegistry activeRunRegistry;
    private final ShutdownGatePort shutdownGatePort;

    private final RetryPolicy retryPolicy = new RetryPolicy();
    private final Map<String, Semaphore> perJobLimit = new ConcurrentHashMap<>();

    public void execute(JobDefinition definition, JobRun run) {
        // Check shutdown gate before starting
        if (shutdownGatePort.isShuttingDown()) {
            log.info("Shutdown in progress. Rejecting run {}", run.id().value());
            return;
        }
        executeLocked(definition, run);
    }

    private void executeLocked(JobDefinition definition, JobRun run) {
        RetrySpec retrySpec = definition.executionConfig().retrySpec();
        Timeout runTimeout = definition.executionConfig().timeout();
        Throwable lastError = null;
        boolean success = false;
        boolean abortExecution = false;

        NodeInstanceId nodeId = nodeRegistry.nodeId();

        run.markStarted(nodeId, Instant.now());

        if (!runRepository.startIfPending(run, nodeId, run.pullEvents())) {
            log.warn("Failed to start run {}. Ownership lost or already running.", run.id().value());
            return;
        }

        Thread executionThread = Thread.currentThread();

        // Register the orchestrator thread and get a handle to update the handler future
        ActiveRunRegistry.RunExecutionHandle executionHandle = activeRunRegistry.register(run.id(), executionThread);

        JobHeartbeatService.HeartbeatHandle heartbeatHandle = heartbeatService.start(
                run.id(), Duration.ofSeconds(10), executionThread
        );

        try {
            for (int attemptNumber = 1; attemptNumber <= retrySpec.maxAttempts(); attemptNumber++) {
                AttemptResult result = runAttempt(definition, run, attemptNumber, runTimeout, nodeId, executionHandle);
                if (result.success()) {
                    success = true;
                    break;
                }
                if (result.abortExecution()) {
                    abortExecution = true;
                    break;
                }

                lastError = result.error();

                if (lastError instanceof JobTimeoutException && run.status() == RunStatus.TIMED_OUT) {
                    break;
                }

                if (attemptNumber < retrySpec.maxAttempts()) {
                    Duration backoff = retryPolicy.backoff(retrySpec, attemptNumber + 1);
                    try {
                        Thread.sleep(backoff.toMillis());
                    } catch (InterruptedException e) {
                        // Cancellation is reported distinctly. Interrupted backoff means abort.
                        Thread.currentThread().interrupt();
                        abortExecution = true;
                        break;
                    }
                }
            }

            if (!abortExecution) {
                finalizeRun(definition, run, success, lastError, nodeId);
            } else {
                log.warn("Run {} aborted due to interruption or ownership loss. Aborting without saving aggregate state.", run.id().value());
            }
        } finally {
            heartbeatHandle.stop();
            // Unregister only after the entire run lifecycle (including retries/backoff) is complete
            activeRunRegistry.unregister(run.id());
        }
    }

    private AttemptResult runAttempt(JobDefinition definition, JobRun run, int attemptNumber, Timeout runTimeout, NodeInstanceId nodeId, ActiveRunRegistry.RunExecutionHandle executionHandle) {
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
            permitAcquired = true;

            JobHandler handler = handlerRegistry.get(definition.jobHandlerKey());
            Future<Object> future = virtualThreadExecutor.submit(() -> handler.execute(RunContext.of(run, attemptNumber, TraceContext.empty())));

            // Update the registry handle so the future can be cancelled during shutdown
            executionHandle.setHandlerFuture(future);

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
                // Clear the future from the handle when the attempt is done
                executionHandle.setHandlerFuture(null);
            }

        } catch (Throwable throwable) {
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
        if (run.status() != RunStatus.TIMED_OUT) {
            if (success) {
                run.succeed(null, Instant.now());
            } else {
                run.fail(lastError != null ? lastError : new JobExecutionException("unknown"), Instant.now());
            }
        }

        boolean saved = runRepository.saveIfOwnedAndRunning(run, nodeId, run.pullEvents());
        if (!saved) {
            log.warn("Failed to finalize run {}. Ownership lost or already reclaimed.", run.id().value());
        }
        observabilityPort.incrementCounter(success ? "job.success" : "job.failed", "job", definition.id().value());
    }

    // Renamed ownershipLost to abortExecution for clarity
    private record AttemptResult(boolean success, Throwable error, boolean abortExecution) {
    }
}