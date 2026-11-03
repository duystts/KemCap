package com.englishplatform.catalog.internal.enrollment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherAssignmentHistoryRepository
    extends JpaRepository<TeacherAssignmentHistory, UUID> {

  List<TeacherAssignmentHistory> findByEnrollmentIdOrderByCreatedAt(UUID enrollmentId);

  /** FR-031: lý do là văn bản tự do, có thể nhắc học viên; giữ ai đổi và khi nào. */
  @Modifying
  @Query(
      """
      update TeacherAssignmentHistory h set h.reason = null
      where h.reason is not null
        and h.enrollmentId in (select e.id from Enrollment e where e.studentId = :studentId)
      """)
  int clearReasonsOf(@Param("studentId") UUID studentId);
}
