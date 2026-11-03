package com.englishplatform.learning.internal.feedback;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Học viên đánh giá buổi học (FR-022); không sửa được sau khi gửi. */
@Entity
@Table(name = "session_feedback")
public class SessionFeedback extends BaseEntity {

  @Column(name = "session_id", nullable = false, updatable = false)
  private UUID sessionId;

  @Column(name = "student_id", nullable = false, updatable = false)
  private UUID studentId;

  @Column(name = "teacher_id", nullable = false, updatable = false)
  private UUID teacherId;

  @Column(nullable = false, updatable = false)
  private short stars;

  private String comment;

  protected SessionFeedback() {}

  SessionFeedback(UUID sessionId, UUID studentId, UUID teacherId, int stars, String comment) {
    this.sessionId = sessionId;
    this.studentId = studentId;
    this.teacherId = teacherId;
    this.stars = (short) stars;
    this.comment = comment == null || comment.isBlank() ? null : comment.trim();
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

  public int getStars() {
    return stars;
  }

  public String getComment() {
    return comment;
  }
}
