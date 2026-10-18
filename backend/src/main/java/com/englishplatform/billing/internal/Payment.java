package com.englishplatform.billing.internal;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Khoản thu chỉ thêm, không sửa/xóa (SC-007, NFR-REL-03). Nhập nhầm thì thêm bản ghi điều chỉnh
 * ({@code is_adjustment}, số tiền âm, ghi chú bắt buộc).
 */
@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

  public enum Method {
    BANK_TRANSFER,
    CASH,
    OTHER
  }

  @Column(name = "enrollment_id", nullable = false, updatable = false)
  private UUID enrollmentId;

  @Column(name = "student_id", nullable = false, updatable = false)
  private UUID studentId;

  @Column(name = "amount_vnd", nullable = false, updatable = false)
  private long amountVnd;

  @Column(name = "paid_at", nullable = false, updatable = false)
  private LocalDate paidAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Method method;

  @Column(name = "is_adjustment", nullable = false, updatable = false)
  private boolean adjustment;

  @Column(updatable = false)
  private String note;

  @Column(name = "recorded_by", nullable = false, updatable = false)
  private UUID recordedBy;

  protected Payment() {}

  Payment(
      UUID enrollmentId,
      UUID studentId,
      long amountVnd,
      LocalDate paidAt,
      Method method,
      boolean adjustment,
      String note,
      UUID recordedBy) {
    this.enrollmentId = enrollmentId;
    this.studentId = studentId;
    this.amountVnd = amountVnd;
    this.paidAt = paidAt;
    this.method = method;
    this.adjustment = adjustment;
    this.note = note;
    this.recordedBy = recordedBy;
  }

  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public long getAmountVnd() {
    return amountVnd;
  }

  public LocalDate getPaidAt() {
    return paidAt;
  }

  public Method getMethod() {
    return method;
  }

  public boolean isAdjustment() {
    return adjustment;
  }

  public String getNote() {
    return note;
  }

  public UUID getRecordedBy() {
    return recordedBy;
  }
}
