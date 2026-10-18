package com.englishplatform.billing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/** Công nợ của một ghi danh; công nợ = {@code amount_due − amount_paid} (> 0 là còn nợ). */
@Entity
@Table(name = "receivable")
public class Receivable implements Persistable<UUID> {

  @Id
  @Column(name = "enrollment_id")
  private UUID enrollmentId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "amount_due_vnd", nullable = false)
  private long amountDueVnd;

  @Column(name = "amount_paid_vnd", nullable = false)
  private long amountPaidVnd;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Transient private boolean isNew = true;

  protected Receivable() {}

  public Receivable(UUID enrollmentId, UUID studentId, long amountDueVnd) {
    this.enrollmentId = enrollmentId;
    this.studentId = studentId;
    this.amountDueVnd = amountDueVnd;
  }

  public long debtVnd() {
    return amountDueVnd - amountPaidVnd;
  }

  /** Cộng một khoản thu (âm với điều chỉnh). */
  void addPaid(long amountVnd) {
    amountPaidVnd += amountVnd;
  }

  @Override
  public UUID getId() {
    return enrollmentId;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PrePersist
  void onPrePersist() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void onPreUpdate() {
    updatedAt = Instant.now();
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    isNew = false;
  }

  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public long getAmountDueVnd() {
    return amountDueVnd;
  }

  public long getAmountPaidVnd() {
    return amountPaidVnd;
  }
}
