package org.javid.schedulean.application.port.out;

/**
 * Port for controlling the intake of new job runs during shutdown.
 * Used by ExecuteJobService to reject new work immediately when shutdown begins.
 */
public interface ShutdownGatePort {
    /**
     * Returns true if the system is shutting down and new runs should be rejected.
     */
    boolean isShuttingDown();
}