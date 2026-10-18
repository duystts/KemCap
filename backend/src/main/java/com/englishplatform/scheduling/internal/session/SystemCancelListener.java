package com.englishplatform.scheduling.internal.session;

import com.englishplatform.catalog.CatalogEvents.TeacherReassigned;
import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.identity.UserDeactivated;
import com.englishplatform.scheduling.SchedulingEvents.SessionCancelled;
import com.englishplatform.scheduling.SessionStatus;
import java.time.Clock;
import java.time.Instant;
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
    cancel(
        sessions
            .findByEnrollmentIdAndStatusAndStartsAtAfter(
                event.enrollmentId(), SessionStatus.BOOKED, now)
            .stream()
            .filter(s -> s.getTeacherId().equals(event.fromTeacherId()))
            .toList(),
        now);
  }

  @ApplicationModuleListener
  void on(UserDeactivated event) {
    if (event.role() == Role.STUDENT) {
      cancelStudentSessions(event.userId());
    }
  }

  @ApplicationModuleListener
  void on(StudentAnonymized event) {
    cancelStudentSessions(event.studentId());
  }

  private void cancelStudentSessions(java.util.UUID studentId) {
    Instant now = clock.instant();
    cancel(
        sessions.findByStudentIdAndStatusAndStartsAtAfter(studentId, SessionStatus.BOOKED, now),
        now);
  }

  private void cancel(List<Session> candidates, Instant now) {
    for (Session s : candidates) {
      s.cancelBySystem(now)
          .ifPresent(
              action -> {
                action.apply(enrollmentApi, s.getEnrollmentId());
                events.publishEvent(
                    new SessionCancelled(
                        s.getId(),
                        s.getEnrollmentId(),
                        SessionStatus.CANCELLED_SYSTEM,
                        null,
                        s.getStartsAt()));
              });
    }
  }
}
