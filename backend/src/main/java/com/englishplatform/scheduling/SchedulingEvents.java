package com.englishplatform.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Event do scheduling phát — payload đúng contracts/events-and-ports.md §1. */
public final class SchedulingEvents {

  private SchedulingEvents() {}

  public record SessionBooked(
      UUID sessionId,
      UUID enrollmentId,
      UUID teacherId,
      UUID studentId,
      Instant startsAt,
      Instant endsAt) {}

  /** Lý do hủy hệ thống ({@code CANCELLED_SYSTEM}); {@code null} khi người dùng tự hủy. */
  public enum CancelCause {
    TEACHER_REASSIGNED,
    USER_DEACTIVATED,
    ANONYMIZED
  }

  public record SessionCancelled(
      UUID sessionId,
      UUID enrollmentId,
      SessionStatus status,
      UUID cancelledBy,
      Instant startsAt,
      CancelCause cause) {}

  /**
   * Phát sau khi đã hủy xong các buổi tương lai với giáo viên cũ (FR-006) — kể cả khi không có buổi
   * nào; notification gửi một thông báo gộp thay cho {@code TeacherReassigned}.
   */
  public record ReassignmentSessionsCancelled(
      UUID enrollmentId,
      UUID studentId,
      UUID fromTeacherId,
      UUID toTeacherId,
      UUID reassignmentId,
      List<Instant> cancelledStartsAt) {}

  public record SessionRescheduled(UUID oldSessionId, UUID newSessionId, Instant newStartsAt) {}

  public enum ReminderWindow {
    H24,
    H1
  }

  public record SessionReminderDue(
      UUID sessionId, UUID teacherId, UUID studentId, Instant startsAt, ReminderWindow window) {}

  public record SessionFinished(
      UUID sessionId,
      UUID enrollmentId,
      UUID teacherId,
      UUID studentId,
      SessionStatus outcome,
      Instant endsAt) {}

  public record SessionOutcomeOverdue(UUID sessionId, UUID teacherId, Instant endsAt) {}
}
