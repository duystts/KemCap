package com.englishplatform.learning.internal;

import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.learning.internal.path.LearningPathRepository;
import com.englishplatform.learning.internal.proficiency.AssessmentRepository;
import com.englishplatform.learning.internal.proficiency.GoalRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Ẩn danh hóa (FR-031): xóa đánh giá năng lực, mục tiêu, lộ trình (chặng, chủ đề, lịch sử sửa theo
 * ON DELETE CASCADE) của học viên. Idempotent — xóa lần hai không còn gì để xóa.
 */
@Component
class ProfileAnonymizationListener {

  private final AssessmentRepository assessments;
  private final GoalRepository goals;
  private final LearningPathRepository paths;

  ProfileAnonymizationListener(
      AssessmentRepository assessments, GoalRepository goals, LearningPathRepository paths) {
    this.assessments = assessments;
    this.goals = goals;
    this.paths = paths;
  }

  @ApplicationModuleListener
  void on(StudentAnonymized event) {
    assessments.deleteByStudent(event.studentId());
    goals.deleteByStudent(event.studentId());
    paths.deleteByStudent(event.studentId());
  }
}
