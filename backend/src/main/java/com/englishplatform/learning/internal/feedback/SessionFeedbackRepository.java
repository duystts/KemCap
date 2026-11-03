package com.englishplatform.learning.internal.feedback;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, UUID> {

  Optional<SessionFeedback> findBySessionId(UUID sessionId);

  boolean existsBySessionId(UUID sessionId);

  List<SessionFeedback> findByStudentId(UUID studentId);

  /** FR-031: xóa nhận xét, giữ số sao. */
  @Modifying
  @Query(
      "update SessionFeedback f set f.comment = null"
          + " where f.studentId = :studentId and f.comment is not null")
  int clearCommentsOf(@Param("studentId") UUID studentId);
}
