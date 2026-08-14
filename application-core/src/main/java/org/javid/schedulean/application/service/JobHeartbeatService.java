package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javid.schedulean.application.port.out.ActiveExecutionRegistry;
import org.javid.schedulean.application.port.out.JobRunUpdatePort;
import org.javid.schedulean.application.port.out.NodeRegistryPort;
import org.javid.schedulean.application.port.out.ObservabilityPort;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class JobHeartbeatService {

    private final JobRunUpdatePort jobRunUpdatePort;
    private final NodeRegistryPort nodeRegistryPort;
    private final ObservabilityPort observabilityPort;

    /**
     * Accepts the execution thread to enable immediate interruption on ownership loss.
     */
    public HeartbeatHandle start(JobRunId runId, Duration interval, Thread orchestratorThread, ActiveExecutionRegistry executionRegistry) {
        AtomicBoolean running = new AtomicBoolean(true);
        NodeInstanceId nodeId = nodeRegistryPort.nodeId();

        Thread beater = Thread.ofVirtual().start(() -> {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(interval.toMillis());

                    // Check if heartbeat was accepted (run is still RUNNING)
                    boolean accepted = jobRunUpdatePort.updateHeartbeat(runId, nodeId, Instant.now());
                    if (!accepted) {
                        log.warn("Heartbeat rejected for run {} - run is no longer RUNNING. Interrupting execution.", runId.value());
                        executionRegistry.cancel(runId); // Interrupts the handler virtual thread
                        orchestratorThread.interrupt();  // Unblocks future.get() or sleep() in the orchestrator
                        running.set(false);
                        break;
                    }
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // No silent failures. Log and emit metric.
                    log.error("Failed to update heartbeat for run: {}", runId.value(), e);
                    observabilityPort.incrementCounter("job.heartbeat.failure", "runId", runId.value());

                    // Add retry delay to prevent 100% CPU spin
                    try {
                        Thread.sleep(interval.toMillis());
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        return new HeartbeatHandle(beater, running);
    }

    public record HeartbeatHandle(Thread thread, AtomicBoolean running) {
        public void stop() {
            running.set(false);
            thread.interrupt();
        }
    }
}
