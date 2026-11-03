package com.englishplatform.learning.internal.assignment;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Bài tập giao kèm phiếu đánh giá (FR-020); hạn nộp tùy chọn. Bị xóa khi ẩn danh hóa. */
@Entity
@Table(name = "assignment")
public class Assignment extends BaseEntity {

  @Column(name = "session_report_id", nullable = false, updatable = false)
  private UUID sessionReportId;

  @Column(name = "session_id", nullable = false, updatable = false)
  private UUID sessionId;

  @Column(name = "student_id", nullable = false, updatable = false)
  private UUID studentId;

  @Column(name = "teacher_id", nullable = false, updatable = false)
  private UUID teacherId;

  @Column(nullable = false)
  private String title;

  private String instructions;

  @Column(name = "due_at")
  private Instant dueAt;

  protected Assignment() {}

  public Assignment(
      UUID sessionReportId,
      UUID sessionId,
      UUID studentId,
      UUID teacherId,
      String title,
      String instructions,
      Instant dueAt) {
    this.sessionReportId = sessionReportId;
    this.sessionId = sessionId;
    this.studentId = studentId;
    this.teacherId = teacherId;
    this.title = title;
    this.instructions = instructions == null || instructions.isBlank() ? null : instructions.trim();
    this.dueAt = dueAt;
  }

  /** Nộp sau hạn vẫn được nhận, chỉ đánh dấu muộn; không có hạn thì không bao giờ muộn. */
  public boolean isLate(Instant submittedAt) {
    return dueAt != null && submittedAt.isAfter(dueAt);
  }

  public UUID getSessionReportId() {
    return sessionReportId;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public UUID getTeacherId() {
    return teacherId;
  }

  public String getTitle() {
    return title;
  }

  public String getInstructions() {
    return instructions;
  }

  public Instant getDueAt() {
    return dueAt;
  }
}
