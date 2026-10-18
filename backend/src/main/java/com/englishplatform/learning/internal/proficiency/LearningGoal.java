package com.englishplatform.learning.internal.proficiency;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Mục tiêu học tập (tùy chọn, 0..n mỗi học viên). Nội dung mục tiêu không sửa — đổi mục tiêu = đóng
 * mục tiêu cũ ({@code ACHIEVED}/{@code DROPPED}) và tạo mục tiêu mới, để giữ lịch sử.
 */
@Entity
@Table(name = "learning_goal")
public class LearningGoal extends BaseEntity {

  public enum Type {
    IELTS,
    TOEIC,
    COMMUNICATION,
    BUSINESS,
    KIDS,
    OTHER
  }

  public enum Status {
    ACTIVE,
    ACHIEVED,
    DROPPED
  }

  @Column(name = "student_id", nullable = false, updatable = false)
  private UUID studentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Type type;

  @Column(nullable = false, updatable = false)
  private String target;

  private LocalDate deadline;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.ACTIVE;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  protected LearningGoal() {}

  public LearningGoal(
      UUID studentId, Type type, String target, LocalDate deadline, UUID createdBy) {
    this.studentId = studentId;
    this.type = type;
    this.target = target.trim();
    this.deadline = deadline;
    this.createdBy = createdBy;
  }

  /** Chỉ đổi trường được gửi (null = giữ nguyên). */
  public void update(Status status, LocalDate deadline) {
    if (status != null) {
      this.status = status;
    }
    if (deadline != null) {
      this.deadline = deadline;
    }
  }

  public UUID getStudentId() {
    return studentId;
  }

  public Type getType() {
    return type;
  }

  public String getTarget() {
    return target;
  }

  public LocalDate getDeadline() {
    return deadline;
  }

  public Status getStatus() {
    return status;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }
}
