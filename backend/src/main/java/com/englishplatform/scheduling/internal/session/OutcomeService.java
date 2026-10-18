package com.englishplatform.scheduling.internal.session;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.scheduling.SchedulingEvents.SessionFinished;
import com.englishplatform.scheduling.SessionStatus;
import com.englishplatform.scheduling.internal.SchedulingAccess;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chốt kết quả buổi (FR-014): giáo viên của buổi, hoặc admin chốt thay; admin sửa kết quả sai trong
 * 7 ngày; admin quyết định buổi Gián đoạn (tính là đã học / xếp bù). Mọi thay đổi được ghi vào
 * {@code session_outcome_change} (ai, từ → sang, ghi chú).
 */
@Service
public class OutcomeService {

  private final SessionLookup lookup;
  private final SessionRepository sessions;
  private final SessionOutcomeChangeRepository changes;
  private final EnrollmentApi enrollmentApi;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  OutcomeService(
      SessionLookup lookup,
      SessionRepository sessions,
      SessionOutcomeChangeRepository changes,
      EnrollmentApi enrollmentApi,
      ApplicationEventPublisher events,
      Clock clock) {
    this.lookup = lookup;
    this.sessions = sessions;
    this.changes = changes;
    this.enrollmentApi = enrollmentApi;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public Session record(CurrentUser actor, UUID sessionId, SessionStatus outcome, String note) {
    Session session = lookup.visible(actor, sessionId);
    if (SchedulingAccess.isStudent(actor)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    SessionStatus from = session.getStatus();
    Optional<CounterAction> action;
    if (from == SessionStatus.BOOKED) {
      action = Optional.of(session.recordOutcome(outcome, actor.id(), clock.instant()));
    } else if (SchedulingAccess.isAdmin(actor)) {
      action = session.correctOutcome(outcome, actor.id(), clock.instant());
    } else {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Buổi đã chốt kết quả; liên hệ giáo vụ để sửa");
    }
    applyAndFlush(session, action);
    changes.save(new SessionOutcomeChange(session.getId(), from, outcome, actor.id(), note));
    publishFinished(session);
    return session;
  }

  /** {@code count = true}: Gián đoạn → {@code COMPLETED} + tính buổi; ngược lại xếp bù. */
  @Transactional
  public Session decideInterruption(CurrentUser actor, UUID sessionId, boolean count) {
    Session session = lookup.visible(actor, sessionId);
    Optional<CounterAction> action = session.decideInterruption(count, actor.id(), clock.instant());
    if (action.isPresent()) {
      applyAndFlush(session, action);
      changes.save(
          new SessionOutcomeChange(
              session.getId(),
              SessionStatus.INTERRUPTED,
              SessionStatus.COMPLETED,
              actor.id(),
              "Gián đoạn: tính là đã học"));
      publishFinished(session);
    }
    return session;
  }

  @Transactional(readOnly = true)
  public List<SessionOutcomeChange> history(CurrentUser actor, UUID sessionId) {
    lookup.visible(actor, sessionId);
    return changes.findBySessionIdOrderByCreatedAt(sessionId);
  }

  /** Buổi đã qua giờ kết thúc mà chưa chốt: admin mọi buổi, giáo viên buổi của mình. */
  @Transactional(readOnly = true)
  public List<Session> pending(CurrentUser actor) {
    UUID teacher = SchedulingAccess.isAdmin(actor) ? null : actor.id();
    return sessions.findOutcomePending(clock.instant(), teacher);
  }

  /**
   * Đổi bộ đếm rồi flush ngay: sửa sang trạng thái chiếm khung giờ có thể vướng exclusion
   * constraint (khung đã có buổi khác) → 409 {@code SLOT_TAKEN} thay vì 500.
   */
  private void applyAndFlush(Session session, Optional<CounterAction> action) {
    try {
      action.ifPresent(a -> a.apply(enrollmentApi, session.getEnrollmentId()));
      sessions.flush();
    } catch (RuntimeException e) {
      if (BookingService.isExclusionViolation(e)) {
        throw new BusinessException(
            ErrorCode.SLOT_TAKEN, "Khung giờ của buổi này đã có buổi khác, không đổi được kết quả");
      }
      throw e;
    }
  }

  private void publishFinished(Session s) {
    events.publishEvent(
        new SessionFinished(
            s.getId(),
            s.getEnrollmentId(),
            s.getTeacherId(),
            s.getStudentId(),
            s.getStatus(),
            s.getEndsAt()));
  }
}
