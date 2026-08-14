package org.javid.schedulean.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javid.schedulean.application.port.out.*;
import org.javid.schedulean.domain.model.JobRun;
import org.javid.schedulean.domain.valueobject.JobRunId;
import org.javid.schedulean.domain.valueobject.NodeInstanceId;
import org.javid.schedulean.domain.valueobject.enums.RunStatus;

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

        // Split abandoned dispatch from execution recovery
        if (run.status() == RunStatus.PENDING) {
            run.markAbandoned(Instant.now(), candidate.reason());
            boolean saved = runRepository.failIfPending(run, staleBefore, run.pullEvents());
            if (saved) {
                log.info("Successfully marked abandoned run {} as FAILED.", runId.value());
            } else {
                log.debug("Abandoned run {} was already claimed or updated by another node.", runId.value());
            }
            return;
        }

        // If the run is already RECOVERING (stranded recovery process), skip beginRecovery
        // and transitionToRecovering, and directly attempt to reclaim it to FAILED.
        if (run.status() != RunStatus.RECOVERING) {
            run.beginRecovery(recoveringNodeId, Instant.now());
            boolean claimed = runRepository.transitionToRecovering(run, recoveringNodeId, staleBefore, run.pullEvents());
            if (!claimed) {
                log.debug("Run {} was already claimed or updated by another node.", runId.value());
                return;
            }
        }

        run.reclaim(candidate.reason(), Instant.now());
        boolean saved = runRepository.saveIfRecoveringAndOwned(run, recoveringNodeId, run.pullEvents());
        if (saved) {
            log.info("Successfully reclaimed run {}.", runId.value());
        } else {
            log.warn("Failed to finalize reclaim for run {}. Lost ownership during recovery.", runId.value());
        }
    }
}
