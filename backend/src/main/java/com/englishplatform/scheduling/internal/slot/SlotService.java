package com.englishplatform.scheduling.internal.slot;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.scheduling.internal.SchedulingAccess;
import com.englishplatform.scheduling.internal.SchedulingProperties;
import com.englishplatform.scheduling.internal.availability.AvailabilityExceptionRepository;
import com.englishplatform.scheduling.internal.availability.AvailabilityRuleRepository;
import com.englishplatform.scheduling.internal.session.Session;
import com.englishplatform.scheduling.internal.session.SessionRepository;
import com.englishplatform.scheduling.internal.slot.SlotGenerator.Busy;
import com.englishplatform.scheduling.internal.slot.SlotGenerator.Slot;
import com.englishplatform.scheduling.internal.slot.SlotGenerator.Window;
import com.englishplatform.shared.security.CurrentUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Khung giờ trống của giáo viên phụ trách một ghi danh (FR-009). */
@Service
public class SlotService {

  private final EnrollmentApi enrollmentApi;
  private final AvailabilityRuleRepository rules;
  private final AvailabilityExceptionRepository exceptions;
  private final SessionRepository sessions;
  private final SchedulingAccess access;
  private final SchedulingProperties props;
  private final Clock clock;

  SlotService(
      EnrollmentApi enrollmentApi,
      AvailabilityRuleRepository rules,
      AvailabilityExceptionRepository exceptions,
      SessionRepository sessions,
      SchedulingAccess access,
      SchedulingProperties props,
      Clock clock) {
    this.enrollmentApi = enrollmentApi;
    this.rules = rules;
    this.exceptions = exceptions;
    this.sessions = sessions;
    this.access = access;
    this.props = props;
    this.clock = clock;
  }

  /** Admin hoặc học viên chủ ghi danh; ghi danh không {@code ACTIVE} → không có slot. */
  @Transactional(readOnly = true)
  public List<Slot> slots(CurrentUser actor, UUID enrollmentId, Instant from, Instant to) {
    EnrollmentView enrollment = enrollmentApi.get(enrollmentId);
    if (!SchedulingAccess.isAdmin(actor) && !actor.id().equals(enrollment.studentId())) {
      throw SchedulingAccess.notFound();
    }
    if (enrollment.status() != EnrollmentStatus.ACTIVE) {
      return List.of();
    }
    Window window = SlotGenerator.window(from, to, clock.instant(), enrollment.expiresAt(), props);
    List<Busy> busy =
        sessions
            .findOccupying(
                enrollment.teacherId(),
                enrollment.studentId(),
                Session.OCCUPYING,
                window.from(),
                window.to())
            .stream()
            .map(s -> new Busy(s.getStartsAt(), s.getEndsAt()))
            .toList();
    return generate(enrollment, window, busy);
  }

  /**
   * {@code startsAt} có phải một slot hợp lệ theo lịch rảnh, ngày nghỉ, thời gian đặt trước và hạn
   * gói không. Không xét buổi bận — trùng giờ do exclusion constraint chặn ({@code SLOT_TAKEN}).
   */
  @Transactional(readOnly = true)
  public boolean isBookable(EnrollmentView enrollment, Instant startsAt) {
    Duration length = Duration.ofMinutes(enrollment.sessionMinutes());
    Window window =
        SlotGenerator.window(
            startsAt, startsAt.plus(length), clock.instant(), enrollment.expiresAt(), props);
    return generate(enrollment, window, List.of()).stream()
        .anyMatch(s -> s.startsAt().equals(startsAt));
  }

  private List<Slot> generate(EnrollmentView enrollment, Window window, List<Busy> busy) {
    if (!window.from().isBefore(window.to())) {
      return List.of();
    }
    ZoneId zone = access.teacherZone(enrollment.teacherId());
    return SlotGenerator.generate(
        window,
        zone,
        Duration.ofMinutes(enrollment.sessionMinutes()),
        rules.findByTeacherIdOrderByDayOfWeekAscStartTimeAsc(enrollment.teacherId()),
        exceptions.findByTeacherIdAndDateBetweenOrderByDate(
            enrollment.teacherId(),
            window.from().atZone(zone).toLocalDate(),
            window.to().atZone(zone).toLocalDate()),
        busy);
  }
}
