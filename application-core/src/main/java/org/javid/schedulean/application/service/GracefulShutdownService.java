package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javid.schedulean.application.port.out.ActiveExecutionRegistry;
import org.javid.schedulean.application.port.out.NodeRegistryPort;
import org.javid.schedulean.application.port.out.OrphanedJobRecoveryPort;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class GracefulShutdownService {

    private final OrphanedJobRecoveryPort recoveryPort;
    private final NodeRegistryPort registry;
    private final ActiveExecutionRegistry activeExecutionRegistry;

    /**
     * Executes a bounded graceful drain.
     *
     * @param drainTimeout Maximum time to wait for active handlers to complete.
     */
    public void executeShutdown(Duration drainTimeout) {
        log.info("Starting graceful shutdown sequence. Waiting up to {} for active executions to drain.", drainTimeout);

        Instant deadline = Instant.now().plus(drainTimeout);

        // Real drain loop. Wait until active executions are zero or deadline is reached.
        while (Instant.now().isBefore(deadline) && activeExecutionRegistry.activeCount() > 0) {
            try {
                Thread.sleep(100); // Poll interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (activeExecutionRegistry.activeCount() > 0) {
            log.warn("Drain timeout expired. {} executions still active. Forcing interruption.", activeExecutionRegistry.activeCount());
            activeExecutionRegistry.cancelAll();
        }

        log.info("Marking remaining local runs as orphaned...");
        recoveryPort.markRunningRunsAsOrphaned(registry.nodeId());

        log.info("Deregistering from cluster...");
        registry.deregister();

        log.info("Graceful shutdown complete.");
    }
}
