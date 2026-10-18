package com.englishplatform.catalog.internal.enrollment;

import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.shared.persistence.BaseEntity;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ghi danh: bộ đếm buổi + máy trạng thái (data-model.md). {@code sessions_total}, {@code
 * session_minutes}, {@code validity_days}, {@code price_vnd} sao chép từ gói lúc tạo và không đổi
 * khi gói bị sửa giá; {@code price_vnd} không có thao tác sửa sau khi tạo.
 */
@Entity
@Table(name = "enrollment")
public class Enrollment extends BaseEntity {

  /** Trạng thái mà giáo viên vẫn "được phân công" học viên (FR-003). */
  public static final Set<EnrollmentStatus> ASSIGNED =
      EnumSet.of(
          EnrollmentStatus.PENDING_PAYMENT,
          EnrollmentStatus.ACTIVE,
          EnrollmentStatus.EXHAUSTED,
          EnrollmentStatus.EXPIRED);

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "package_id", nullable = false)
  private UUID packageId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "sessions_total", nullable = false)
  private int sessionsTotal;

  @Column(name = "session_minutes", nullable = false)
  private int sessionMinutes;

  @Column(name = "validity_days", nullable = false)
  private int validityDays;

  @Column(name = "price_vnd", nullable = false)
  private long priceVnd;

  @Column(name = "sessions_reserved", nullable = false)
  private int sessionsReserved;

  @Column(name = "sessions_consumed", nullable = false)
  private int sessionsConsumed;

  @Column(name = "activated_at")
  private Instant activatedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EnrollmentStatus status = EnrollmentStatus.PENDING_PAYMENT;

  @Column(name = "renewal_low_notified_at")
  private Instant renewalLowNotifiedAt;

  @Column(name = "renewal_expiring_notified_at")
  private Instant renewalExpiringNotifiedAt;

  @Column(name = "extended_by")
  private UUID extendedBy;

  @Column(name = "extended_at")
  private Instant extendedAt;

  @Version private long version;

  protected Enrollment() {}

  public Enrollment(
      UUID studentId,
      UUID packageId,
      UUID teacherId,
      int sessionsTotal,
      int sessionMinutes,
      int validityDays,
      long priceVnd) {
    this.studentId = studentId;
    this.packageId = packageId;
    this.teacherId = teacherId;
    this.sessionsTotal = sessionsTotal;
    this.sessionMinutes = sessionMinutes;
    this.validityDays = validityDays;
    this.priceVnd = priceVnd;
  }

  public int sessionsRemaining() {
    return sessionsTotal - sessionsReserved - sessionsConsumed;
  }

  /** Đặt lịch: chỉ khi {@code ACTIVE}, chưa hết hạn và còn buổi (FR-024). */
  public void reserve(Instant now) {
    if (status != EnrollmentStatus.ACTIVE || (expiresAt != null && !now.isBefore(expiresAt))) {
      throw new BusinessException(ErrorCode.ENROLLMENT_NOT_ACTIVE);
    }
    if (sessionsRemaining() <= 0) {
      throw new BusinessException(ErrorCode.NO_SESSIONS_LEFT);
    }
    sessionsReserved++;
  }

  /** Hủy đúng hạn / giáo viên vắng / gián đoạn / hủy hệ thống: trả lại chỗ. */
  public void release() {
    requireReserved();
    sessionsReserved--;
  }

  /** Hoàn thành / học viên vắng / hủy muộn: chuyển một chỗ đã đặt sang đã dùng. */
  public void consume() {
    requireReserved();
    sessionsReserved--;
    sessionsConsumed++;
    exhaustIfUsedUp();
  }

  /** Gián đoạn được tính là đã học: chỗ đã trả trước đó, nên cần còn một buổi trống. */
  public void consumeReleased() {
    if (sessionsRemaining() <= 0) {
      throw new BusinessException(ErrorCode.NO_SESSIONS_LEFT);
    }
    sessionsConsumed++;
    exhaustIfUsedUp();
  }

  /** Thanh toán đầu tiên: {@code expires_at = now + validity_days}. */
  public void activate(Instant now) {
    if (status != EnrollmentStatus.PENDING_PAYMENT) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    status = EnrollmentStatus.ACTIVE;
    activatedAt = now;
    expiresAt = now.plus(Duration.ofDays(validityDays));
  }

  /** Job hằng ngày (U8): {@code ACTIVE} quá hạn → {@code EXPIRED}; buổi đã đặt vẫn giữ. */
  public void expire() {
    if (status != EnrollmentStatus.ACTIVE) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    status = EnrollmentStatus.EXPIRED;
  }

  /** Gia hạn thủ công (FR-024): {@code ACTIVE}/{@code EXPIRED} → {@code ACTIVE}. */
  public void extend(Instant newExpiresAt, UUID by, Instant now) {
    if (status != EnrollmentStatus.ACTIVE && status != EnrollmentStatus.EXPIRED) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    if (newExpiresAt == null || !newExpiresAt.isAfter(now)) {
      throw BusinessException.validation("newExpiresAt", "Hạn mới phải sau thời điểm hiện tại");
    }
    expiresAt = newExpiresAt;
    status = EnrollmentStatus.ACTIVE;
    extendedBy = by;
    extendedAt = now;
  }

  /** Hủy ghi danh: chỉ {@code PENDING_PAYMENT}/{@code ACTIVE} và không còn buổi đã đặt. */
  public void cancel() {
    if (status != EnrollmentStatus.PENDING_PAYMENT && status != EnrollmentStatus.ACTIVE) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    if (sessionsReserved > 0) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Ghi danh còn buổi đã đặt, hãy hủy các buổi trước");
    }
    status = EnrollmentStatus.CANCELLED;
  }

  public void changeTeacher(UUID newTeacherId) {
    if (status == EnrollmentStatus.CANCELLED) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    if (teacherId.equals(newTeacherId)) {
      throw BusinessException.validation("teacherId", "Giáo viên mới trùng giáo viên hiện tại");
    }
    teacherId = newTeacherId;
  }

  public EnrollmentView toView() {
    return new EnrollmentView(
        getId(),
        studentId,
        teacherId,
        sessionsTotal,
        sessionsReserved,
        sessionsConsumed,
        sessionMinutes,
        expiresAt,
        status);
  }

  private void requireReserved() {
    if (sessionsReserved <= 0) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
  }

  private void exhaustIfUsedUp() {
    if (sessionsConsumed >= sessionsTotal && status == EnrollmentStatus.ACTIVE) {
      status = EnrollmentStatus.EXHAUSTED;
    }
  }

  public UUID getStudentId() {
    return studentId;
  }

  public UUID getPackageId() {
    return packageId;
  }

  public UUID getTeacherId() {
    return teacherId;
  }

  public int getSessionsTotal() {
    return sessionsTotal;
  }

  public int getSessionMinutes() {
    return sessionMinutes;
  }

  public int getValidityDays() {
    return validityDays;
  }

  public long getPriceVnd() {
    return priceVnd;
  }

  public int getSessionsReserved() {
    return sessionsReserved;
  }

  public int getSessionsConsumed() {
    return sessionsConsumed;
  }

  public Instant getActivatedAt() {
    return activatedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public UUID getExtendedBy() {
    return extendedBy;
  }

  public Instant getExtendedAt() {
    return extendedAt;
  }

  public long getVersion() {
    return version;
  }
}
