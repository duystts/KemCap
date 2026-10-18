package com.englishplatform.scheduling.internal.web;

import com.englishplatform.scheduling.SessionStatus;
import com.englishplatform.scheduling.VideoRoomProvider.JoinInfo;
import com.englishplatform.scheduling.internal.SchedulingAccess;
import com.englishplatform.scheduling.internal.session.BookingService;
import com.englishplatform.scheduling.internal.session.CancellationService;
import com.englishplatform.scheduling.internal.session.JoinService;
import com.englishplatform.scheduling.internal.session.OutcomeService;
import com.englishplatform.scheduling.internal.session.Session;
import com.englishplatform.scheduling.internal.session.SessionLookup;
import com.englishplatform.scheduling.internal.session.SessionRepository;
import com.englishplatform.scheduling.internal.slot.SlotGenerator.Slot;
import com.englishplatform.scheduling.internal.slot.SlotService;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Buổi học, khung giờ trống, vào phòng (contracts/rest-api.md nhóm scheduling). */
@RestController
@RequestMapping("/api/v1")
class SessionController {

  private final BookingService booking;
  private final CancellationService cancellation;
  private final OutcomeService outcomes;
  private final JoinService joins;
  private final SlotService slots;
  private final SessionLookup lookup;
  private final SessionRepository sessions;

  SessionController(
      BookingService booking,
      CancellationService cancellation,
      OutcomeService outcomes,
      JoinService joins,
      SlotService slots,
      SessionLookup lookup,
      SessionRepository sessions) {
    this.booking = booking;
    this.cancellation = cancellation;
    this.outcomes = outcomes;
    this.joins = joins;
    this.slots = slots;
    this.lookup = lookup;
    this.sessions = sessions;
  }

  record SessionDto(
      UUID id,
      UUID enrollmentId,
      UUID teacherId,
      UUID studentId,
      Instant startsAt,
      Instant endsAt,
      SessionStatus status,
      UUID bookedBy,
      UUID cancelledBy,
      String cancelReason,
      UUID rescheduledFromId,
      UUID outcomeBy,
      Instant teacherJoinedAt,
      Instant studentJoinedAt,
      long version) {

    static SessionDto of(Session s) {
      return new SessionDto(
          s.getId(),
          s.getEnrollmentId(),
          s.getTeacherId(),
          s.getStudentId(),
          s.getStartsAt(),
          s.getEndsAt(),
          s.getStatus(),
          s.getBookedBy(),
          s.getCancelledBy(),
          s.getCancelReason(),
          s.getRescheduledFromId(),
          s.getOutcomeBy(),
          s.getTeacherJoinedAt(),
          s.getStudentJoinedAt(),
          s.getVersion());
    }
  }

  record BookRequest(@NotNull UUID enrollmentId, @NotNull Instant startsAt) {}

  record CancelRequest(String reason) {}

  record RescheduleRequest(@NotNull Instant newStartsAt) {}

  record OutcomeRequest(@NotNull SessionStatus outcome, String note) {}

  enum Decision {
    COUNT,
    MAKE_UP
  }

  record InterruptionRequest(@NotNull Decision decision) {}

  @GetMapping("/enrollments/{enrollmentId}/slots")
  @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
  List<Slot> slots(
      @PathVariable UUID enrollmentId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    return slots.slots(CurrentUser.require(), enrollmentId, from, to);
  }

  @PostMapping("/sessions")
  @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
  @ResponseStatus(HttpStatus.CREATED)
  SessionDto book(@Valid @RequestBody BookRequest r) {
    return SessionDto.of(booking.book(CurrentUser.require(), r.enrollmentId(), r.startsAt()));
  }

  /**
   * Lịch theo quyền: giáo viên chỉ buổi của mình, học viên chỉ buổi của mình; {@code
   * outcomePending=true} = buổi đã qua giờ kết thúc mà chưa chốt (A; T).
   */
  @GetMapping("/sessions")
  @PreAuthorize("isAuthenticated()")
  @Transactional(readOnly = true)
  List<SessionDto> list(
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(required = false) UUID teacherId,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) SessionStatus status,
      @RequestParam(required = false) Boolean outcomePending) {
    CurrentUser user = CurrentUser.require();
    if (Boolean.TRUE.equals(outcomePending)) {
      if (SchedulingAccess.isStudent(user)) {
        return List.of();
      }
      return outcomes.pending(user).stream().map(SessionDto::of).toList();
    }
    if (SchedulingAccess.isTeacher(user)) {
      teacherId = user.id();
    } else if (SchedulingAccess.isStudent(user)) {
      studentId = user.id();
    }
    return sessions.search(teacherId, studentId, status, from, to).stream()
        .map(SessionDto::of)
        .toList();
  }

  @GetMapping("/sessions/{id}")
  @PreAuthorize("isAuthenticated()")
  @Transactional(readOnly = true)
  SessionDto get(@PathVariable UUID id) {
    return SessionDto.of(lookup.visible(CurrentUser.require(), id));
  }

  @GetMapping("/sessions/{id}/cancel-preview")
  @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
  CancellationService.Preview cancelPreview(@PathVariable UUID id) {
    return cancellation.preview(CurrentUser.require(), id);
  }

  @PostMapping("/sessions/{id}/cancel")
  @PreAuthorize("isAuthenticated()")
  SessionDto cancel(@PathVariable UUID id, @RequestBody(required = false) CancelRequest r) {
    return SessionDto.of(
        cancellation.cancel(CurrentUser.require(), id, r == null ? null : r.reason()));
  }

  @PostMapping("/sessions/{id}/reschedule")
  @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
  SessionDto reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleRequest r) {
    return SessionDto.of(cancellation.reschedule(CurrentUser.require(), id, r.newStartsAt()));
  }

  @PostMapping("/sessions/{id}/outcome")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  SessionDto outcome(@PathVariable UUID id, @Valid @RequestBody OutcomeRequest r) {
    return SessionDto.of(outcomes.record(CurrentUser.require(), id, r.outcome(), r.note()));
  }

  record OutcomeChangeDto(
      SessionStatus fromStatus,
      SessionStatus toStatus,
      UUID changedBy,
      String note,
      Instant changedAt) {}

  /** Nhật ký chốt/sửa kết quả (admin) — NFR-PRIV-04. */
  @GetMapping("/sessions/{id}/outcome-history")
  @PreAuthorize("hasRole('ADMIN')")
  List<OutcomeChangeDto> outcomeHistory(@PathVariable UUID id) {
    return outcomes.history(CurrentUser.require(), id).stream()
        .map(
            c ->
                new OutcomeChangeDto(
                    c.getFromStatus(),
                    c.getToStatus(),
                    c.getChangedBy(),
                    c.getNote(),
                    c.getCreatedAt()))
        .toList();
  }

  @PostMapping("/sessions/{id}/interruption-decision")
  @PreAuthorize("hasRole('ADMIN')")
  SessionDto interruptionDecision(
      @PathVariable UUID id, @Valid @RequestBody InterruptionRequest r) {
    return SessionDto.of(
        outcomes.decideInterruption(CurrentUser.require(), id, r.decision() == Decision.COUNT));
  }

  @PostMapping("/sessions/{id}/join")
  @PreAuthorize("hasAnyRole('TEACHER','STUDENT')")
  JoinInfo join(@PathVariable UUID id) {
    return joins.join(CurrentUser.require(), id);
  }
}
