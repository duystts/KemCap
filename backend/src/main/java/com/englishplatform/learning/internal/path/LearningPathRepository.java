package com.englishplatform.learning.internal.path;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningPathRepository extends JpaRepository<LearningPath, UUID> {

  Optional<LearningPath> findByStudentIdAndStatus(UUID studentId, LearningPath.Status status);

  /** Ẩn danh hóa (FR-031): chặng, chủ đề, lịch sử sửa bị xóa theo ON DELETE CASCADE. */
  @Modifying
  @Query("delete from LearningPath p where p.studentId = :studentId")
  int deleteByStudent(@Param("studentId") UUID studentId);
}
