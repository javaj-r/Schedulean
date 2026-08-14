package org.javid.schedulean.application.port.out;

/**
 * Port for master election mechanics.
 * This port is intended to be used solely by the MasterElectionClusterModeAdapter
 * to determine cluster leadership. Application core services use ClusterModePort instead.
 */
public interface ElectionPort {
    boolean isMaster();

    boolean tryAcquireMastership();
}
