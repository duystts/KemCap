package com.englishplatform.learning.internal.assignment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

  List<Assignment> findByStudentIdInOrderByCreatedAtDesc(Collection<UUID> studentIds);

  List<Assignment> findAllByOrderByCreatedAtDesc();

  /** Ẩn danh hóa (FR-031): bài nộp bị xóa theo ON DELETE CASCADE. */
  @Modifying
  @Query("delete from Assignment a where a.studentId = :studentId")
  int deleteByStudent(@Param("studentId") UUID studentId);
}
