package com.englishplatform.catalog.internal.enrollment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherAssignmentHistoryRepository
    extends JpaRepository<TeacherAssignmentHistory, UUID> {

  List<TeacherAssignmentHistory> findByEnrollmentIdOrderByCreatedAt(UUID enrollmentId);
}
