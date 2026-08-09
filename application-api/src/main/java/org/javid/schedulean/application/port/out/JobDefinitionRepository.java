package org.javid.schedulean.application.port.out;

import org.javid.schedulean.domain.model.JobDefinition;
import org.javid.schedulean.domain.valueobject.JobId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobDefinitionRepository {
    JobDefinition save(JobDefinition def);

    Optional<JobDefinition> findById(JobId id);

    List<JobDefinition> findAllActive();

    List<JobDefinition> findDueJobs(Instant now);

    void deleteById(JobId id);
}
