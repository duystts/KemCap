package com.englishplatform.learning.internal.assignment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

  Optional<Submission> findByAssignmentId(UUID assignmentId);

  List<Submission> findByAssignmentIdIn(Collection<UUID> assignmentIds);
}
