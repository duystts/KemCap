package com.englishplatform.catalog.internal;

import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import com.englishplatform.catalog.internal.enrollment.TeacherAssignmentHistoryRepository;
import com.englishplatform.catalog.internal.profile.StudentProfileRepository;
import com.englishplatform.identity.StudentAnonymized;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Ẩn danh hóa (FR-031): xóa {@code notes}, {@code self_reported_level}, {@code occupation}, lý do
 * đổi giáo viên và lý do hủy ghi danh. Idempotent — Event Publication Registry có thể phát lại khi
 * khởi động lại.
 */
@Component
class AnonymizationListener {

  private final StudentProfileRepository profiles;
  private final TeacherAssignmentHistoryRepository assignmentHistory;
  private final EnrollmentRepository enrollments;

  AnonymizationListener(
      StudentProfileRepository profiles,
      TeacherAssignmentHistoryRepository assignmentHistory,
      EnrollmentRepository enrollments) {
    this.profiles = profiles;
    this.assignmentHistory = assignmentHistory;
    this.enrollments = enrollments;
  }

  @ApplicationModuleListener
  void on(StudentAnonymized event) {
    profiles.findById(event.studentId()).ifPresent(profile -> profile.anonymize());
    assignmentHistory.clearReasonsOf(event.studentId());
    enrollments.clearCancelReasonsOf(event.studentId());
  }
}
