package com.englishplatform.learning.internal;

import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.learning.internal.assignment.AssignmentRepository;
import com.englishplatform.learning.internal.feedback.SessionFeedbackRepository;
import com.englishplatform.learning.internal.report.SessionReportRepository;
import java.time.Clock;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Ẩn danh hóa (FR-031): xóa bài tập và bài nộp; trong phiếu xóa điểm, nhận xét, chủ đề (giữ mốc
 * thời gian); trong đánh giá sao xóa nhận xét (giữ số sao). Idempotent.
 */
@Component
class ReportAnonymizationListener {

  private final AssignmentRepository assignments;
  private final SessionReportRepository reports;
  private final SessionFeedbackRepository feedback;
  private final Clock clock;

  ReportAnonymizationListener(
      AssignmentRepository assignments,
      SessionReportRepository reports,
      SessionFeedbackRepository feedback,
      Clock clock) {
    this.assignments = assignments;
    this.reports = reports;
    this.feedback = feedback;
    this.clock = clock;
  }

  @ApplicationModuleListener
  void on(StudentAnonymized event) {
    assignments.deleteByStudent(event.studentId());
    reports.anonymizeForStudent(event.studentId(), clock.instant());
    feedback.clearCommentsOf(event.studentId());
  }
}
