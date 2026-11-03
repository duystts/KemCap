package com.englishplatform.learning.internal.web;

import com.englishplatform.learning.internal.assignment.Assignment;
import com.englishplatform.learning.internal.assignment.AssignmentService;
import com.englishplatform.learning.internal.assignment.Submission;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bài tập và bài nộp (contracts/rest-api.md nhóm learning, FR-020; K2 chỉ văn bản). */
@RestController
@RequestMapping("/api/v1/assignments")
class AssignmentController {

  private final AssignmentService service;

  AssignmentController(AssignmentService service) {
    this.service = service;
  }

  record SubmissionDto(
      String textAnswer,
      Instant submittedAt,
      boolean late,
      Integer score,
      String feedback,
      UUID gradedBy,
      Instant gradedAt) {}

  record AssignmentDto(
      UUID id,
      UUID sessionId,
      UUID studentId,
      UUID teacherId,
      String title,
      String instructions,
      Instant dueAt,
      AssignmentService.Status status,
      SubmissionDto submission) {

    static AssignmentDto of(AssignmentService.View v) {
      Assignment a = v.assignment();
      Submission s = v.submission();
      return new AssignmentDto(
          a.getId(),
          a.getSessionId(),
          a.getStudentId(),
          a.getTeacherId(),
          a.getTitle(),
          a.getInstructions(),
          a.getDueAt(),
          v.status(),
          s == null
              ? null
              : new SubmissionDto(
                  s.getTextAnswer(),
                  s.getSubmittedAt(),
                  a.isLate(s.getSubmittedAt()),
                  s.getScore(),
                  s.getFeedback(),
                  s.getGradedBy(),
                  s.getGradedAt()));
    }
  }

  record SubmitRequest(String textAnswer) {}

  record GradeRequest(@NotNull Integer score, String feedback) {}

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  List<AssignmentDto> list(
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) AssignmentService.Status status) {
    return service.list(CurrentUser.require(), studentId, status).stream()
        .map(AssignmentDto::of)
        .toList();
  }

  @PutMapping("/{id}/submission")
  @PreAuthorize("hasRole('STUDENT')")
  AssignmentDto submit(@PathVariable UUID id, @RequestBody SubmitRequest r) {
    return AssignmentDto.of(service.submit(CurrentUser.require(), id, r.textAnswer()));
  }

  @PostMapping("/{id}/grade")
  @PreAuthorize("hasRole('TEACHER')")
  AssignmentDto grade(@PathVariable UUID id, @Valid @RequestBody GradeRequest r) {
    return AssignmentDto.of(service.grade(CurrentUser.require(), id, r.score(), r.feedback()));
  }
}
