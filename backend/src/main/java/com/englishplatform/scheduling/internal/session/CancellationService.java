package com.englishplatform.scheduling.internal.session;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.scheduling.SchedulingEvents.SessionCancelled;
import com.englishplatform.scheduling.SchedulingEvents.SessionRescheduled;
import com.englishplatform.scheduling.internal.SchedulingAccess;
import com.englishplatform.scheduling.internal.SchedulingProperties;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hủy / đổi lịch (FR-011). Hạn chót = {@code app.scheduling.cancel-cutoff-hours} trước giờ học (chỉ
 * qua cấu hình triển khai). Giáo viên hủy = {@code TEACHER_ABSENT}.
 */
@Service
public class CancellationService {

  private final SessionRepository sessions;
  private final SessionLookup lookup;
  private final BookingService bookingService;
  private final EnrollmentApi enrollmentApi;
  private final SchedulingProperties props;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  CancellationService(
      SessionRepository sessions,
      SessionLookup lookup,
      BookingService bookingService,
      EnrollmentApi enrollmentApi,
      SchedulingProperties props,
      ApplicationEventPublisher events,
      Clock clock) {
    this.sessions = sessions;
    this.lookup = lookup;
    this.bookingService = bookingService;
    this.enrollmentApi = enrollmentApi;
    this.props = props;
    this.events = events;
    this.clock = clock;
  }

  public record Preview(boolean willConsume, Instant cutoffAt) {}

  @Transactional(readOnly = true)
  public Preview preview(CurrentUser actor, UUID sessionId) {
    Session session = studentOrAdmin(actor, sessionId);
    return new Preview(
        session.wouldConsumeIfCancelled(clock.instant(), props.cancelCutoff()),
        session.cutoffAt(props.cancelCutoff()));
  }

  @Transactional
  public Session cancel(CurrentUser actor, UUID sessionId, String reason) {
    Session session = lookup.visible(actor, sessionId);
    CounterAction action =
        SchedulingAccess.isTeacher(actor)
            ? session.cancelByTeacher(actor.id(), reason)
            : session.cancel(clock.instant(), props.cancelCutoff(), actor.id(), reason);
    action.apply(enrollmentApi, session.getEnrollmentId());
    events.publishEvent(
        new SessionCancelled(
            session.getId(),
            session.getEnrollmentId(),
            session.getStatus(),
            actor.id(),
            session.getStartsAt(),
            null));
    return session;
  }

  /** Hủy đúng hạn buổi cũ + đặt buổi mới trong một transaction; quá hạn chót → 409. */
  @Transactional
  public Session reschedule(CurrentUser actor, UUID sessionId, Instant newStartsAt) {
    Session old = studentOrAdmin(actor, sessionId);
    Instant now = clock.instant();
    if (old.wouldConsumeIfCancelled(now, props.cancelCutoff())) {
      throw new BusinessException(ErrorCode.CANCEL_WINDOW_PASSED);
    }
    old.cancel(now, props.cancelCutoff(), actor.id(), "Đổi lịch")
        .apply(enrollmentApi, old.getEnrollmentId());
    sessions.flush();
    Session next = bookingService.create(actor, old.getEnrollmentId(), newStartsAt, old.getId());
    events.publishEvent(new SessionRescheduled(old.getId(), next.getId(), next.getStartsAt()));
    return next;
  }

  private Session studentOrAdmin(CurrentUser actor, UUID sessionId) {
    Session session = lookup.visible(actor, sessionId);
    if (SchedulingAccess.isTeacher(actor)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return session;
  }
}
