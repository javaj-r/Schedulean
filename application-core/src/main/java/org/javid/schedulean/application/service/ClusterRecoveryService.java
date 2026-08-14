package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javid.schedulean.application.port.out.*;
import org.javid.schedulean.domain.model.JobRun;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ClusterRecoveryService {

    private final ClusterModePort clusterModePort;
    private final NodeRegistryPort nodeRegistryPort;
    private final OrphanedJobRecoveryPort recoveryPort;
    private final JobRunRepository runRepository;
    private final ObservabilityPort observabilityPort;
    private final RecoveryPolicy recoveryPolicy;

    public void recoveryCycle() {
        if (!clusterModePort.canRunRecovery()) {
            return;
        }

        Instant staleBefore = Instant.now().minus(recoveryPolicy.staleAfter());
        // Receive typed candidates. Includes both RUNNING/PENDING (stale) and RECOVERING (stranded) candidates.
        List<OrphanedJobRecoveryPort.RecoveryCandidate> candidates = recoveryPort.findRecoveryCandidates(staleBefore);

        for (OrphanedJobRecoveryPort.RecoveryCandidate candidate : candidates) {
            if (!clusterModePort.canRunRecovery()) {
                log.info("Lost recovery permission during cycle. Aborting remaining recoveries.");
                break;
            }

            try {
                reclaimRun(candidate, staleBefore);
            } catch (Exception e) {
                log.error("Failed to reclaim run {}", candidate.runId().value(), e);
                observabilityPort.incrementCounter("job.recovery.failure", "runId", candidate.runId().value());
            }
        }
    }

    private void reclaimRun(OrphanedJobRecoveryPort.RecoveryCandidate candidate, Instant staleBefore) {
        JobRunId runId = candidate.runId();
        NodeInstanceId recoveringNodeId = nodeRegistryPort.nodeId();

        JobRun run = runRepository.findById(runId).orElse(null);
        if (run == null) {
            log.warn("Cannot reclaim run {} because it was not found in the repository.", runId.value());
            return;
        }

        // Aggregate generates JobRecoveryStarted event
        run.beginRecovery(recoveringNodeId, Instant.now());

        // Atomically transition DB to RECOVERING and persist outbox event.
        // This prevents stale workers from overwriting state.
        boolean claimed = runRepository.transitionToRecovering(run, recoveringNodeId, staleBefore, run.pullEvents());
        if (!claimed) {
            log.debug("Run {} was already claimed or updated by another node.", runId.value());
            return;
        }

        // Aggregate generates JobReclaimed event
        run.reclaim(candidate.reason(), Instant.now());

        // Atomically transition DB to FAILED and persist outbox event.
        boolean saved = runRepository.saveIfRecoveringAndOwned(run, recoveringNodeId, run.pullEvents());
        if (saved) {
            log.info("Successfully reclaimed run {}.", runId.value());
        } else {
            log.warn("Failed to finalize reclaim for run {}. Lost ownership during recovery.", runId.value());
        }
    }
}
