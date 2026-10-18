package com.englishplatform.catalog.internal.web;

import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.internal.CatalogAccess;
import com.englishplatform.catalog.internal.enrollment.Enrollment;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.catalog.internal.enrollment.TeacherAssignmentHistory;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/enrollments} — ghi danh (FR-006, FR-024). Không có API sửa giá: {@code priceVnd}
 * chỉ nhập lúc tạo.
 */
@RestController
@RequestMapping("/api/v1/enrollments")
class EnrollmentController {

  private final EnrollmentService service;

  EnrollmentController(EnrollmentService service) {
    this.service = service;
  }

  /** {@code priceVnd} chỉ trả cho admin và học viên chủ ghi danh (giáo viên không cần học phí). */
  record EnrollmentDto(
      UUID id,
      UUID studentId,
      UUID teacherId,
      UUID packageId,
      int sessionsTotal,
      int sessionsReserved,
      int sessionsConsumed,
      int sessionsRemaining,
      int sessionMinutes,
      Long priceVnd,
      Instant activatedAt,
      Instant expiresAt,
      EnrollmentStatus status,
      long version) {

    static EnrollmentDto of(Enrollment e, CurrentUser viewer) {
      boolean showPrice = !CatalogAccess.isTeacher(viewer);
      return new EnrollmentDto(
          e.getId(),
          e.getStudentId(),
          e.getTeacherId(),
          e.getPackageId(),
          e.getSessionsTotal(),
          e.getSessionsReserved(),
          e.getSessionsConsumed(),
          e.sessionsRemaining(),
          e.getSessionMinutes(),
          showPrice ? e.getPriceVnd() : null,
          e.getActivatedAt(),
          e.getExpiresAt(),
          e.getStatus(),
          e.getVersion());
    }
  }

  record AssignmentDto(
      UUID fromTeacherId, UUID toTeacherId, UUID changedBy, String reason, Instant changedAt) {
    static AssignmentDto of(TeacherAssignmentHistory h) {
      return new AssignmentDto(
          h.getFromTeacherId(),
          h.getToTeacherId(),
          h.getChangedBy(),
          h.getReason(),
          h.getCreatedAt());
    }
  }

  record CreateEnrollmentRequest(
      @NotNull UUID studentId, @NotNull UUID packageId, @NotNull UUID teacherId, Long priceVnd) {}

  record ChangeTeacherRequest(@NotNull UUID teacherId, @NotBlank String reason) {}

  record ExtendRequest(@NotNull Instant newExpiresAt) {}

  record CancelRequest(String reason) {}

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  EnrollmentDto create(@Valid @RequestBody CreateEnrollmentRequest r) {
    CurrentUser user = CurrentUser.require();
    return EnrollmentDto.of(
        service.create(r.studentId(), r.packageId(), r.teacherId(), r.priceVnd(), user.id()), user);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  List<EnrollmentDto> list(
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID teacherId,
      @RequestParam(required = false) EnrollmentStatus status) {
    CurrentUser user = CurrentUser.require();
    return service.list(user, studentId, teacherId, status).stream()
        .map(e -> EnrollmentDto.of(e, user))
        .toList();
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  EnrollmentDto get(@PathVariable UUID id) {
    CurrentUser user = CurrentUser.require();
    return EnrollmentDto.of(service.get(user, id), user);
  }

  @GetMapping("/{id}/teacher-history")
  @PreAuthorize("hasRole('ADMIN')")
  List<AssignmentDto> teacherHistory(@PathVariable UUID id) {
    return service.history(id).stream().map(AssignmentDto::of).toList();
  }

  @PutMapping("/{id}/teacher")
  @PreAuthorize("hasRole('ADMIN')")
  EnrollmentDto changeTeacher(@PathVariable UUID id, @Valid @RequestBody ChangeTeacherRequest r) {
    CurrentUser user = CurrentUser.require();
    return EnrollmentDto.of(service.changeTeacher(id, r.teacherId(), r.reason(), user.id()), user);
  }

  @PostMapping("/{id}/extend")
  @PreAuthorize("hasRole('ADMIN')")
  EnrollmentDto extend(@PathVariable UUID id, @Valid @RequestBody ExtendRequest r) {
    CurrentUser user = CurrentUser.require();
    return EnrollmentDto.of(service.extend(id, r.newExpiresAt(), user.id()), user);
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('ADMIN')")
  EnrollmentDto cancel(@PathVariable UUID id, @RequestBody(required = false) CancelRequest r) {
    return EnrollmentDto.of(service.cancel(id), CurrentUser.require());
  }
}
