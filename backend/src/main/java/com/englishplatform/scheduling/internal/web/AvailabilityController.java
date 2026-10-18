package com.englishplatform.scheduling.internal.web;

import com.englishplatform.scheduling.internal.availability.AvailabilityException;
import com.englishplatform.scheduling.internal.availability.AvailabilityRule;
import com.englishplatform.scheduling.internal.availability.AvailabilityService;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/teachers/{id}/availability} — admin hoặc chính giáo viên (FR-008). */
@RestController
@RequestMapping("/api/v1/teachers/{teacherId}/availability")
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
class AvailabilityController {

  private final AvailabilityService service;

  AvailabilityController(AvailabilityService service) {
    this.service = service;
  }

  /** {@code dayOfWeek}: MONDAY … SUNDAY; giờ theo múi giờ của giáo viên. */
  record RuleDto(
      @NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {
    static RuleDto of(AvailabilityRule r) {
      return new RuleDto(r.getDayOfWeek(), r.getStartTime(), r.getEndTime());
    }
  }

  record ExceptionDto(
      UUID id, LocalDate date, LocalTime startTime, LocalTime endTime, String reason) {
    static ExceptionDto of(AvailabilityException e) {
      return new ExceptionDto(
          e.getId(), e.getDate(), e.getStartTime(), e.getEndTime(), e.getReason());
    }
  }

  record ExceptionRequest(
      @NotNull LocalDate date,
      LocalTime startTime,
      LocalTime endTime,
      @Size(max = 200) String reason) {}

  @GetMapping
  List<RuleDto> rules(@PathVariable UUID teacherId) {
    return service.rules(CurrentUser.require(), teacherId).stream().map(RuleDto::of).toList();
  }

  @PutMapping
  List<RuleDto> replace(@PathVariable UUID teacherId, @Valid @RequestBody List<RuleDto> rules) {
    return service
        .replaceRules(
            CurrentUser.require(),
            teacherId,
            rules.stream()
                .map(
                    r ->
                        new AvailabilityService.RuleSpec(r.dayOfWeek(), r.startTime(), r.endTime()))
                .toList())
        .stream()
        .map(RuleDto::of)
        .toList();
  }

  @GetMapping("/exceptions")
  List<ExceptionDto> exceptions(@PathVariable UUID teacherId) {
    return service.exceptions(CurrentUser.require(), teacherId).stream()
        .map(ExceptionDto::of)
        .toList();
  }

  @PostMapping("/exceptions")
  @ResponseStatus(HttpStatus.CREATED)
  ExceptionDto addException(@PathVariable UUID teacherId, @Valid @RequestBody ExceptionRequest r) {
    return ExceptionDto.of(
        service.addException(
            CurrentUser.require(),
            teacherId,
            new AvailabilityService.ExceptionSpec(
                r.date(), r.startTime(), r.endTime(), r.reason())));
  }

  @DeleteMapping("/exceptions/{exceptionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteException(@PathVariable UUID teacherId, @PathVariable UUID exceptionId) {
    service.deleteException(CurrentUser.require(), teacherId, exceptionId);
  }
}
