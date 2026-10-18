package com.englishplatform.catalog.internal.enrollment;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Lịch sử phân công giáo viên của một ghi danh (FR-006); {@code from_teacher_id} null ở lần đầu.
 */
@Entity
@Table(name = "teacher_assignment_history")
public class TeacherAssignmentHistory extends BaseEntity {

  @Column(name = "enrollment_id", nullable = false)
  private UUID enrollmentId;

  @Column(name = "from_teacher_id")
  private UUID fromTeacherId;

  @Column(name = "to_teacher_id", nullable = false)
  private UUID toTeacherId;

  @Column(name = "changed_by", nullable = false)
  private UUID changedBy;

  private String reason;

  protected TeacherAssignmentHistory() {}

  public TeacherAssignmentHistory(
      UUID enrollmentId, UUID fromTeacherId, UUID toTeacherId, UUID changedBy, String reason) {
    this.enrollmentId = enrollmentId;
    this.fromTeacherId = fromTeacherId;
    this.toTeacherId = toTeacherId;
    this.changedBy = changedBy;
    this.reason = reason;
  }

  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  public UUID getFromTeacherId() {
    return fromTeacherId;
  }

  public UUID getToTeacherId() {
    return toTeacherId;
  }

  public UUID getChangedBy() {
    return changedBy;
  }

  public String getReason() {
    return reason;
  }
}
