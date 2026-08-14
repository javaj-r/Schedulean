package org.javid.schedulean.application.port.out;

public interface ClusterModePort {
    boolean canPollJobs();

    /**
     * Decouples recovery execution from NodeRegistryPort.isMaster().
     * In ShedLock mode, this may return true for all nodes; in Master-Election mode, only the master returns true.
     */
    boolean canRunRecovery();
}
