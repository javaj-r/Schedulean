package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javid.schedulean.application.port.out.*;import org.javid.schedulean.domain.valueobject.ShutdownPolicy;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class GracefulShutdownService implements ShutdownGatePort {

    private final IntakeControlPort intakeControlPort;
    private final OrphanedJobRecoveryPort recoveryPort;
    private final NodeRegistryPort registry;
    private final ActiveRunRegistry activeRunRegistry;

    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    /**
     * Executes a bounded graceful drain.
     *
     * @param policy Configurable timings for the drain sequence.
     */
    public void executeShutdown(ShutdownPolicy policy) {

        // Make shutdown idempotent
        if (!isShuttingDown.compareAndSet(false, true)) {
            log.info("Shutdown already in progress. Ignoring duplicate request.");
            return;
        }

        log.info("Starting graceful shutdown sequence. Waiting up to {} for active runs to drain.", policy.drainTimeout());

        // Stop intake (REST, NATS, Poller)
        try {
            intakeControlPort.stopIntake();
        } catch (Exception e) {
            // Do not continue after intake-stop failure. Fail fast to prevent new work during drain.
            log.error("Failed to stop intake. Aborting graceful shutdown to prevent data loss.", e);
            isShuttingDown.set(false); // Rollback gate so application can continue running
            throw new IllegalStateException("Failed to stop intake during shutdown", e);
        }

        Instant deadline = Instant.now().plus(policy.drainTimeout());

        // Wait for active runs (orchestrators) to complete naturally
        while (Instant.now().isBefore(deadline) && activeRunRegistry.activeCount() > 0) {
            try {
                Thread.sleep(policy.pollInterval().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Force cancel remaining runs (orchestrators AND handlers)
        if (activeRunRegistry.activeCount() > 0) {
            log.warn("Drain timeout expired. {} active runs still present. Forcing interruption.", activeRunRegistry.activeCount());
            activeRunRegistry.cancelAll();

            // Add a second bounded wait after cancellation to allow cooperative interruption to finish
            Instant cancelDeadline = Instant.now().plus(policy.postCancelGracePeriod());
            while (Instant.now().isBefore(cancelDeadline) && activeRunRegistry.activeCount() > 0) {
                try {
                    Thread.sleep(policy.pollInterval().toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Mark remaining local runs as orphaned
        try {
            log.info("Marking remaining local runs as orphaned...");
            // Adapter implementation MUST fence: WHERE status = 'RUNNING' AND executing_node_id = ?
            recoveryPort.markRunningRunsAsOrphaned(registry.nodeId());
        } catch (Exception e) {
            log.error("Failed to mark runs as orphaned. They will be recovered by the stale heartbeat scan.", e);
        } finally {
            // Ensure final node deregistration is attempted in a finally block
            log.info("Deregistering from cluster...");
            try {
                registry.deregister();
            } catch (Exception e) {
                log.error("Failed to deregister from cluster. Other nodes will detect this node as dead via heartbeat.", e);
            }
        }

        log.info("Graceful shutdown complete.");
    }

    /**
     * Implementation of ShutdownGatePort.
     * Allows ExecuteJobService to check if intake is stopped.
     */
    @Override
    public boolean isShuttingDown() {
        return isShuttingDown.get();
    }
}
