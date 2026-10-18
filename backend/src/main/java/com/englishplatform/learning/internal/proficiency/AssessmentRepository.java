package com.englishplatform.learning.internal.proficiency;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Đánh giá chỉ thêm và đọc — không có update/delete lẻ (NFR-PORT-03). */
public interface AssessmentRepository extends Repository<ProficiencyAssessment, UUID> {

  ProficiencyAssessment save(ProficiencyAssessment assessment);

  List<ProficiencyAssessment> findByStudentIdOrderByAssessedAtDescCreatedAtDesc(UUID studentId);

  /** Ẩn danh hóa (FR-031). */
  @Modifying
  @Query("delete from ProficiencyAssessment a where a.studentId = :studentId")
  int deleteByStudent(@Param("studentId") UUID studentId);
}
