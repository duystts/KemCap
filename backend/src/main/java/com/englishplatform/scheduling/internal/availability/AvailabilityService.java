package com.englishplatform.scheduling.internal.availability;

import com.englishplatform.scheduling.SessionStatus;
import com.englishplatform.scheduling.internal.SchedulingAccess;
import com.englishplatform.scheduling.internal.session.Session;
import com.englishplatform.scheduling.internal.session.SessionRepository;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lịch rảnh hằng tuần + ngày nghỉ của giáo viên (FR-008). Đổi lịch rảnh không ảnh hưởng buổi đã
 * đặt; ngày nghỉ trùng buổi đã đặt bị từ chối kèm danh sách buổi.
 */
@Service
public class AvailabilityService {

  private final AvailabilityRuleRepository rules;
  private final AvailabilityExceptionRepository exceptions;
  private final SessionRepository sessions;
  private final SchedulingAccess access;
  private final Clock clock;

  AvailabilityService(
      AvailabilityRuleRepository rules,
      AvailabilityExceptionRepository exceptions,
      SessionRepository sessions,
      SchedulingAccess access,
      Clock clock) {
    this.rules = rules;
    this.exceptions = exceptions;
    this.sessions = sessions;
    this.access = access;
    this.clock = clock;
  }

  public record RuleSpec(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {}

  public record ExceptionSpec(
      LocalDate date, LocalTime startTime, LocalTime endTime, String reason) {}

  @Transactional(readOnly = true)
  public List<AvailabilityRule> rules(CurrentUser actor, UUID teacherId) {
    SchedulingAccess.requireAdminOrSelf(actor, teacherId);
    access.requireTeacher(teacherId);
    return rules.findByTeacherIdOrderByDayOfWeekAscStartTimeAsc(teacherId);
  }

  /** Thay toàn bộ quy tắc; từ chối khoảng giờ chồng nhau trong cùng một ngày. */
  @Transactional
  public List<AvailabilityRule> replaceRules(
      CurrentUser actor, UUID teacherId, List<RuleSpec> specs) {
    SchedulingAccess.requireAdminOrSelf(actor, teacherId);
    access.requireTeacher(teacherId);
    List<RuleSpec> sorted =
        specs.stream()
            .sorted(Comparator.comparing(RuleSpec::dayOfWeek).thenComparing(RuleSpec::startTime))
            .toList();
    for (int i = 0; i < sorted.size(); i++) {
      RuleSpec r = sorted.get(i);
      if (r.dayOfWeek() == null || r.startTime() == null || r.endTime() == null) {
        throw BusinessException.validation("rules", "Thiếu ngày hoặc giờ");
      }
      if (!r.endTime().isAfter(r.startTime())) {
        throw BusinessException.validation("rules", "Giờ kết thúc phải sau giờ bắt đầu");
      }
      if (i > 0
          && sorted.get(i - 1).dayOfWeek() == r.dayOfWeek()
          && sorted.get(i - 1).endTime().isAfter(r.startTime())) {
        throw BusinessException.validation(
            "rules", "Các khung giờ trong cùng một ngày bị chồng nhau");
      }
    }
    rules.deleteByTeacher(teacherId);
    return rules.saveAll(
        sorted.stream()
            .map(r -> new AvailabilityRule(teacherId, r.dayOfWeek(), r.startTime(), r.endTime()))
            .toList());
  }

  @Transactional(readOnly = true)
  public List<AvailabilityException> exceptions(CurrentUser actor, UUID teacherId) {
    SchedulingAccess.requireAdminOrSelf(actor, teacherId);
    ZoneId zone = access.teacherZone(teacherId);
    LocalDate today = clock.instant().atZone(zone).toLocalDate();
    return exceptions.findByTeacherIdAndDateBetweenOrderByDate(
        teacherId, today, today.plusYears(1));
  }

  /** Trùng buổi {@code BOOKED} → 409 {@code EXCEPTION_OVERLAPS_SESSIONS} kèm {@code sessions[]}. */
  @Transactional
  public AvailabilityException addException(CurrentUser actor, UUID teacherId, ExceptionSpec spec) {
    SchedulingAccess.requireAdminOrSelf(actor, teacherId);
    ZoneId zone = access.teacherZone(teacherId);
    if (spec.date() == null) {
      throw BusinessException.validation("date", "Chọn ngày nghỉ");
    }
    if ((spec.startTime() == null) != (spec.endTime() == null)) {
      throw BusinessException.validation(
          "startTime", "Nhập cả giờ bắt đầu và kết thúc, hoặc bỏ trống cả hai");
    }
    if (spec.startTime() != null && !spec.endTime().isAfter(spec.startTime())) {
      throw BusinessException.validation("endTime", "Giờ kết thúc phải sau giờ bắt đầu");
    }
    Instant from =
        spec.date()
            .atTime(spec.startTime() == null ? LocalTime.MIN : spec.startTime())
            .atZone(zone)
            .toInstant();
    Instant to =
        spec.startTime() == null
            ? spec.date().plusDays(1).atStartOfDay(zone).toInstant()
            : spec.date().atTime(spec.endTime()).atZone(zone).toInstant();
    List<Session> overlapping =
        sessions.findTeacherSessions(teacherId, SessionStatus.BOOKED, from, to);
    if (!overlapping.isEmpty()) {
      throw new BusinessException(ErrorCode.EXCEPTION_OVERLAPS_SESSIONS)
          .withDetail(
              "sessions",
              overlapping.stream()
                  .map(
                      s ->
                          Map.of(
                              "id", s.getId(),
                              "studentId", s.getStudentId(),
                              "startsAt", s.getStartsAt(),
                              "endsAt", s.getEndsAt()))
                  .toList());
    }
    return exceptions.save(
        new AvailabilityException(
            teacherId, spec.date(), spec.startTime(), spec.endTime(), spec.reason()));
  }

  @Transactional
  public void deleteException(CurrentUser actor, UUID teacherId, UUID exceptionId) {
    SchedulingAccess.requireAdminOrSelf(actor, teacherId);
    AvailabilityException ex =
        exceptions
            .findById(exceptionId)
            .filter(e -> e.getTeacherId().equals(teacherId))
            .orElseThrow(SchedulingAccess::notFound);
    exceptions.delete(ex);
  }
}
