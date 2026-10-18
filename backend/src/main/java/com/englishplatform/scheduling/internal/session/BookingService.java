package com.englishplatform.scheduling.internal.session;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.scheduling.SchedulingEvents.SessionBooked;
import com.englishplatform.scheduling.internal.SchedulingAccess;
import com.englishplatform.scheduling.internal.slot.SlotService;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đặt buổi (FR-009, FR-010): slot phải hợp lệ; {@code EnrollmentApi.reserve} và insert session
 * trong <b>cùng transaction</b>; trùng giờ do exclusion constraint chặn → 409 {@code SLOT_TAKEN}.
 */
@Service
public class BookingService {

  /** SQLState của PostgreSQL khi vi phạm exclusion constraint. */
  static final String EXCLUSION_VIOLATION = "23P01";

  private final SessionRepository sessions;
  private final EnrollmentApi enrollmentApi;
  private final SlotService slotService;
  private final ApplicationEventPublisher events;

  BookingService(
      SessionRepository sessions,
      EnrollmentApi enrollmentApi,
      SlotService slotService,
      ApplicationEventPublisher events) {
    this.sessions = sessions;
    this.enrollmentApi = enrollmentApi;
    this.slotService = slotService;
    this.events = events;
  }

  @Transactional
  public Session book(CurrentUser actor, UUID enrollmentId, Instant startsAt) {
    Session session = create(actor, enrollmentId, startsAt, null);
    events.publishEvent(
        new SessionBooked(
            session.getId(),
            enrollmentId,
            session.getTeacherId(),
            session.getStudentId(),
            session.getStartsAt(),
            session.getEndsAt()));
    return session;
  }

  /** Dùng chung cho đặt mới và đổi lịch; chạy trong transaction của bên gọi. */
  @Transactional
  Session create(CurrentUser actor, UUID enrollmentId, Instant startsAt, UUID rescheduledFromId) {
    if (startsAt == null) {
      throw BusinessException.validation("startsAt", "Chọn giờ học");
    }
    EnrollmentView enrollment = enrollmentApi.get(enrollmentId);
    if (!SchedulingAccess.isAdmin(actor) && !actor.id().equals(enrollment.studentId())) {
      throw SchedulingAccess.notFound();
    }
    if (enrollment.status() != EnrollmentStatus.ACTIVE) {
      throw new BusinessException(ErrorCode.ENROLLMENT_NOT_ACTIVE);
    }
    if (!slotService.isBookable(enrollment, startsAt)) {
      throw BusinessException.validation(
          "startsAt", "Giờ học không nằm trong khung giờ trống của giáo viên");
    }
    Instant endsAt = startsAt.plus(java.time.Duration.ofMinutes(enrollment.sessionMinutes()));
    if (!sessions
        .findOccupying(
            enrollment.teacherId(), enrollment.studentId(), Session.OCCUPYING, startsAt, endsAt)
        .isEmpty()) {
      // lượt tuần tự: báo ngay; lượt chen nhau vẫn do exclusion constraint chặn bên dưới
      throw new BusinessException(ErrorCode.SLOT_TAKEN);
    }
    enrollmentApi.reserve(enrollmentId);
    Session session =
        new Session(
            enrollmentId,
            enrollment.teacherId(),
            enrollment.studentId(),
            startsAt,
            enrollment.sessionMinutes(),
            actor.id(),
            rescheduledFromId);
    try {
      return sessions.saveAndFlush(session);
    } catch (DataIntegrityViolationException e) {
      if (isExclusionViolation(e)) {
        throw new BusinessException(ErrorCode.SLOT_TAKEN);
      }
      throw e;
    }
  }

  public static boolean isExclusionViolation(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof SQLException sql && EXCLUSION_VIOLATION.equals(sql.getSQLState())) {
        return true;
      }
    }
    return false;
  }
}
