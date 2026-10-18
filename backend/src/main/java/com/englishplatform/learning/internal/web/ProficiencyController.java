package com.englishplatform.learning.internal.web;

import com.englishplatform.learning.internal.proficiency.LearningGoal;
import com.englishplatform.learning.internal.proficiency.ProficiencyAssessment;
import com.englishplatform.learning.internal.proficiency.ProficiencyAssessment.Cefr;
import com.englishplatform.learning.internal.proficiency.ProficiencyService;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ năng lực và mục tiêu (contracts/rest-api.md nhóm learning). Không có quyền xem → 404; học
 * viên chỉ đọc.
 */
@RestController
@RequestMapping("/api/v1/students/{studentId}")
class ProficiencyController {

  private final ProficiencyService service;

  ProficiencyController(ProficiencyService service) {
    this.service = service;
  }

  record AssessmentDto(
      UUID id,
      ProficiencyAssessment.Type type,
      UUID assessedBy,
      LocalDate assessedAt,
      Cefr listening,
      Cefr speaking,
      Cefr reading,
      Cefr writing,
      Cefr grammarVocab,
      String notes,
      Instant recordedAt) {

    static AssessmentDto of(ProficiencyAssessment a) {
      var s = a.skills();
      return new AssessmentDto(
          a.getId(),
          a.getType(),
          a.getAssessedBy(),
          a.getAssessedAt(),
          s.listening(),
          s.speaking(),
          s.reading(),
          s.writing(),
          s.grammarVocab(),
          a.getNotes(),
          a.getCreatedAt());
    }
  }

  record AssessmentRequest(
      @NotNull ProficiencyAssessment.Type type,
      LocalDate assessedAt,
      @NotNull Cefr listening,
      @NotNull Cefr speaking,
      @NotNull Cefr reading,
      @NotNull Cefr writing,
      @NotNull Cefr grammarVocab,
      String notes) {}

  record ProfileDto(AssessmentDto current, List<AssessmentDto> history, boolean needsConsulting) {}

  record GoalDto(
      UUID id,
      LearningGoal.Type type,
      String target,
      LocalDate deadline,
      LearningGoal.Status status,
      UUID createdBy,
      Instant createdAt) {
    static GoalDto of(LearningGoal g) {
      return new GoalDto(
          g.getId(),
          g.getType(),
          g.getTarget(),
          g.getDeadline(),
          g.getStatus(),
          g.getCreatedBy(),
          g.getCreatedAt());
    }
  }

  record GoalRequest(
      @NotNull LearningGoal.Type type,
      @NotBlank @Size(max = 100) String target,
      LocalDate deadline) {}

  record GoalUpdateRequest(LearningGoal.Status status, LocalDate deadline) {}

  @GetMapping("/assessments")
  @PreAuthorize("isAuthenticated()")
  List<AssessmentDto> assessments(@PathVariable UUID studentId) {
    return service.assessments(CurrentUser.require(), studentId).stream()
        .map(AssessmentDto::of)
        .toList();
  }

  @PostMapping("/assessments")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  @ResponseStatus(HttpStatus.CREATED)
  AssessmentDto assess(@PathVariable UUID studentId, @Valid @RequestBody AssessmentRequest r) {
    return AssessmentDto.of(
        service.assess(
            CurrentUser.require(),
            studentId,
            r.type(),
            r.assessedAt(),
            new ProficiencyAssessment.Skills(
                r.listening(), r.speaking(), r.reading(), r.writing(), r.grammarVocab()),
            r.notes()));
  }

  @GetMapping("/proficiency")
  @PreAuthorize("isAuthenticated()")
  ProfileDto proficiency(@PathVariable UUID studentId) {
    ProficiencyService.Profile p = service.profile(CurrentUser.require(), studentId);
    return new ProfileDto(
        p.current() == null ? null : AssessmentDto.of(p.current()),
        p.history().stream().map(AssessmentDto::of).toList(),
        p.needsConsulting());
  }

  @GetMapping("/goals")
  @PreAuthorize("isAuthenticated()")
  List<GoalDto> goals(@PathVariable UUID studentId) {
    return service.goals(CurrentUser.require(), studentId).stream().map(GoalDto::of).toList();
  }

  @PostMapping("/goals")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  @ResponseStatus(HttpStatus.CREATED)
  GoalDto addGoal(@PathVariable UUID studentId, @Valid @RequestBody GoalRequest r) {
    return GoalDto.of(
        service.addGoal(CurrentUser.require(), studentId, r.type(), r.target(), r.deadline()));
  }

  @PutMapping("/goals/{goalId}")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  GoalDto updateGoal(
      @PathVariable UUID studentId, @PathVariable UUID goalId, @RequestBody GoalUpdateRequest r) {
    return GoalDto.of(
        service.updateGoal(CurrentUser.require(), studentId, goalId, r.status(), r.deadline()));
  }
}
