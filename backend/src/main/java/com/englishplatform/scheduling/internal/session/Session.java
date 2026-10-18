package com.englishplatform.scheduling.internal.session;

import com.englishplatform.scheduling.SessionQueryApi.SessionView;
import com.englishplatform.scheduling.SessionStatus;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Buổi học 1-1. Mỗi phương thức chuyển trạng thái trả {@link CounterAction} để bên gọi đổi bộ đếm
 * ghi danh trong cùng transaction (data-model.md bảng "State &amp; ảnh hưởng tới số buổi").
 */
@Entity
@Table(name = "session")
public class Session extends BaseEntity {

  /** Kết quả giáo viên (hoặc admin chốt thay) được ghi. */
  public static final Set<SessionStatus> OUTCOMES =
      EnumSet.of(
          SessionStatus.COMPLETED,
          SessionStatus.STUDENT_NO_SHOW,
          SessionStatus.INTERRUPTED,
          SessionStatus.TEACHER_ABSENT);

  /** Kết quả bị tính buổi. */
  static final Set<SessionStatus> CHARGED =
      EnumSet.of(
          SessionStatus.COMPLETED, SessionStatus.STUDENT_NO_SHOW, SessionStatus.CANCELLED_LATE);

  /** Trạng thái chiếm khung giờ (cùng điều kiện với exclusion constraint V5). */
  public static final Set<SessionStatus> OCCUPYING =
      EnumSet.of(
          SessionStatus.BOOKED,
          SessionStatus.COMPLETED,
          SessionStatus.STUDENT_NO_SHOW,
          SessionStatus.CANCELLED_LATE,
          SessionStatus.INTERRUPTED);

  static final Duration CORRECTION_WINDOW = Duration.ofDays(7);

  @Column(name = "enrollment_id", nullable = false, updatable = false)
  private UUID enrollmentId;

  @Column(name = "teacher_id", nullable = false, updatable = false)
  private UUID teacherId;

  @Column(name = "student_id", nullable = false, updatable = false)
  private UUID studentId;

  @Column(name = "starts_at", nullable = false, updatable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false, updatable = false)
  private Instant endsAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SessionStatus status = SessionStatus.BOOKED;

  @Column(name = "booked_by", nullable = false, updatable = false)
  private UUID bookedBy;

  @Column(name = "cancelled_by")
  private UUID cancelledBy;

  @Column(name = "cancel_reason")
  private String cancelReason;

  @Column(name = "rescheduled_from_id", updatable = false)
  private UUID rescheduledFromId;

  @Column(name = "teacher_joined_at")
  private Instant teacherJoinedAt;

  @Column(name = "student_joined_at")
  private Instant studentJoinedAt;

  @Column(name = "reminder_24h_at")
  private Instant reminder24hAt;

  @Column(name = "reminder_1h_at")
  private Instant reminder1hAt;

  @Column(name = "outcome_overdue_notified_at")
  private Instant outcomeOverdueNotifiedAt;

  @Column(name = "outcome_by")
  private UUID outcomeBy;

  @Column(name = "outcome_at")
  private Instant outcomeAt;

  @Version private long version;

  protected Session() {}

  public Session(
      UUID enrollmentId,
      UUID teacherId,
      UUID studentId,
      Instant startsAt,
      int sessionMinutes,
      UUID bookedBy,
      UUID rescheduledFromId) {
    this.enrollmentId = enrollmentId;
    this.teacherId = teacherId;
    this.studentId = studentId;
    this.startsAt = startsAt;
    this.endsAt = startsAt.plus(Duration.ofMinutes(sessionMinutes));
    this.bookedBy = bookedBy;
    this.rescheduledFromId = rescheduledFromId;
  }

  /** Hạn chót hủy/đổi lịch không bị tính buổi. */
  public Instant cutoffAt(Duration cutoff) {
    return startsAt.minus(cutoff);
  }

  public boolean wouldConsumeIfCancelled(Instant now, Duration cutoff) {
    return now.isAfter(cutoffAt(cutoff));
  }

  /** Học viên/admin hủy trước giờ học: trước hạn chót → hoàn buổi, sau → tính buổi (FR-011). */
  public CounterAction cancel(Instant now, Duration cutoff, UUID by, String reason) {
    requireBooked();
    if (!now.isBefore(startsAt)) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Buổi học đã bắt đầu, hãy chốt kết quả buổi");
    }
    boolean late = wouldConsumeIfCancelled(now, cutoff);
    close(late ? SessionStatus.CANCELLED_LATE : SessionStatus.CANCELLED_ON_TIME, by, reason);
    return late ? CounterAction.CONSUME : CounterAction.RELEASE;
  }

  /** Giáo viên hủy = giáo viên vắng, luôn hoàn buổi. */
  public CounterAction cancelByTeacher(UUID by, String reason) {
    requireBooked();
    close(SessionStatus.TEACHER_ABSENT, by, reason);
    return CounterAction.RELEASE;
  }

  /**
   * Hệ thống hủy (đổi giáo viên, ngừng kích hoạt/ẩn danh hóa học viên): chỉ buổi {@code BOOKED}
   * chưa bắt đầu; rỗng nếu không cần làm gì (event phát lại, buổi đã qua).
   */
  public Optional<CounterAction> cancelBySystem(Instant now) {
    if (status != SessionStatus.BOOKED || !startsAt.isAfter(now)) {
      return Optional.empty();
    }
    close(SessionStatus.CANCELLED_SYSTEM, null, "Hủy tự động bởi hệ thống");
    return Optional.of(CounterAction.RELEASE);
  }

  /** Giáo viên của buổi hoặc admin chốt kết quả, chỉ sau giờ bắt đầu (FR-014). */
  public CounterAction recordOutcome(SessionStatus outcome, UUID by, Instant now) {
    if (!OUTCOMES.contains(outcome)) {
      throw BusinessException.validation(
          "outcome", "Kết quả chỉ là COMPLETED, STUDENT_NO_SHOW, INTERRUPTED hoặc TEACHER_ABSENT");
    }
    requireBooked();
    if (now.isBefore(startsAt)) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Chỉ chốt kết quả sau giờ bắt đầu buổi học");
    }
    status = outcome;
    outcomeBy = by;
    outcomeAt = now;
    return CHARGED.contains(outcome) ? CounterAction.CONSUME : CounterAction.RELEASE;
  }

  /**
   * Admin sửa kết quả sai trong 7 ngày sau giờ kết thúc; bộ đếm tính lại theo chênh lệch tính/không
   * tính buổi. Chỉ sửa được kết quả đã <b>chốt</b> sau giờ bắt đầu — buổi giáo viên hủy trước giờ
   * học ({@code TEACHER_ABSENT} qua hủy) không phải kết quả buổi học.
   */
  public Optional<CounterAction> correctOutcome(SessionStatus outcome, UUID by, Instant now) {
    if (!OUTCOMES.contains(outcome)
        || !OUTCOMES.contains(status)
        || outcomeAt == null
        || cancelledBy != null
        || now.isBefore(startsAt)) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Chỉ sửa được kết quả đã chốt của buổi đã diễn ra");
    }
    if (now.isAfter(endsAt.plus(CORRECTION_WINDOW))) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Chỉ sửa kết quả trong 7 ngày sau buổi học");
    }
    boolean wasCharged = CHARGED.contains(status);
    boolean charged = CHARGED.contains(outcome);
    status = outcome;
    outcomeBy = by;
    outcomeAt = now;
    if (wasCharged == charged) {
      return Optional.empty();
    }
    return Optional.of(charged ? CounterAction.CONSUME_RELEASED : CounterAction.UNCONSUME);
  }

  /** {@code count}: Gián đoạn tính là đã học → {@code COMPLETED}; ngược lại xếp bù, giữ nguyên. */
  public Optional<CounterAction> decideInterruption(boolean count, UUID by, Instant now) {
    if (status != SessionStatus.INTERRUPTED) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    if (!count) {
      return Optional.empty();
    }
    status = SessionStatus.COMPLETED;
    outcomeBy = by;
    outcomeAt = now;
    return Optional.of(CounterAction.CONSUME_RELEASED);
  }

  public boolean isParticipant(UUID userId) {
    return teacherId.equals(userId) || studentId.equals(userId);
  }

  public SessionView toView() {
    return new SessionView(getId(), enrollmentId, teacherId, studentId, startsAt, endsAt, status);
  }

  private void requireBooked() {
    if (status != SessionStatus.BOOKED) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
  }

  private void close(SessionStatus target, UUID by, String reason) {
    status = target;
    cancelledBy = by;
    cancelReason = reason == null || reason.isBlank() ? null : reason.trim();
  }

  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  public UUID getTeacherId() {
    return teacherId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public SessionStatus getStatus() {
    return status;
  }

  public UUID getBookedBy() {
    return bookedBy;
  }

  public UUID getCancelledBy() {
    return cancelledBy;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public UUID getRescheduledFromId() {
    return rescheduledFromId;
  }

  public Instant getTeacherJoinedAt() {
    return teacherJoinedAt;
  }

  public Instant getStudentJoinedAt() {
    return studentJoinedAt;
  }

  public Instant getReminder24hAt() {
    return reminder24hAt;
  }

  public Instant getReminder1hAt() {
    return reminder1hAt;
  }

  public Instant getOutcomeOverdueNotifiedAt() {
    return outcomeOverdueNotifiedAt;
  }

  public UUID getOutcomeBy() {
    return outcomeBy;
  }

  public Instant getOutcomeAt() {
    return outcomeAt;
  }

  public long getVersion() {
    return version;
  }
}
