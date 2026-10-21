package com.englishplatform.scheduling.internal.session;

import com.englishplatform.catalog.CatalogEvents.TeacherReassigned;
import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.identity.UserDeactivated;
import com.englishplatform.scheduling.SchedulingEvents.CancelCause;
import com.englishplatform.scheduling.SchedulingEvents.ReassignmentSessionsCancelled;
import com.englishplatform.scheduling.SchedulingEvents.SessionCancelled;
import com.englishplatform.scheduling.SessionStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Hủy hệ thống (FR-006, FR-004, FR-031): buổi {@code BOOKED} chưa bắt đầu → {@code
 * CANCELLED_SYSTEM} + trả chỗ. Idempotent — event phát lại chỉ gặp buổi đã hủy nên không làm gì.
 */
@Component
class SystemCancelListener {

  private final SessionRepository sessions;
  private final EnrollmentApi enrollmentApi;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  SystemCancelListener(
      SessionRepository sessions,
      EnrollmentApi enrollmentApi,
      ApplicationEventPublisher events,
      Clock clock) {
    this.sessions = sessions;
    this.enrollmentApi = enrollmentApi;
    this.events = events;
    this.clock = clock;
  }

  /** Đổi giáo viên: buổi tương lai của ghi danh đó với giáo viên cũ. */
  @ApplicationModuleListener
  void on(TeacherReassigned event) {
    Instant now = clock.instant();
    List<Instant> cancelled =
        cancel(
            sessions
                .findByEnrollmentIdAndStatusAndStartsAtAfter(
                    event.enrollmentId(), SessionStatus.BOOKED, now)
                .stream()
                .filter(s -> s.getTeacherId().equals(event.fromTeacherId()))
                .toList(),
            now,
            CancelCause.TEACHER_REASSIGNED);
    events.publishEvent(
        new ReassignmentSessionsCancelled(
            event.enrollmentId(),
            event.studentId(),
            event.fromTeacherId(),
            event.toTeacherId(),
            event.reassignmentId(),
            cancelled));
  }

  @ApplicationModuleListener
  void on(UserDeactivated event) {
    if (event.role() == Role.STUDENT) {
      cancelStudentSessions(event.userId(), CancelCause.USER_DEACTIVATED);
    }
  }

  @ApplicationModuleListener
  void on(StudentAnonymized event) {
    cancelStudentSessions(event.studentId(), CancelCause.ANONYMIZED);
  }

  private void cancelStudentSessions(java.util.UUID studentId, CancelCause cause) {
    Instant now = clock.instant();
    cancel(
        sessions.findByStudentIdAndStatusAndStartsAtAfter(studentId, SessionStatus.BOOKED, now),
        now,
        cause);
  }

  /** Trả về giờ bắt đầu của các buổi thật sự bị hủy lần này (theo thứ tự thời gian). */
  private List<Instant> cancel(List<Session> candidates, Instant now, CancelCause cause) {
    List<Instant> cancelled = new ArrayList<>();
    for (Session s : candidates) {
      s.cancelBySystem(now)
          .ifPresent(
              action -> {
                action.apply(enrollmentApi, s.getEnrollmentId());
                cancelled.add(s.getStartsAt());
                events.publishEvent(
                    new SessionCancelled(
                        s.getId(),
                        s.getEnrollmentId(),
                        SessionStatus.CANCELLED_SYSTEM,
                        null,
                        s.getStartsAt(),
                        cause));
              });
    }
    cancelled.sort(null);
    return cancelled;
  }
}
