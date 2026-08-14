package org.javid.schedulean.application.port.out;

/**
 * Port for controlling the intake of new job runs.
 * Used during graceful shutdown to stop driving adapters (REST, NATS, Poller)
 * from accepting or dispatching new work before draining active executions.
 */
public interface IntakeControlPort {
    /**
     * Stops all driving adapters from accepting new job runs.
     * Implementations MUST block until intake is fully stopped.
     */
    void stopIntake();
}