package com.englishplatform.learning.internal.web;

import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.learning.internal.report.SessionReport;
import com.englishplatform.learning.internal.report.SessionReportService;
import com.englishplatform.shared.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Phiếu đánh giá buổi học (contracts/rest-api.md nhóm learning, FR-019). */
@RestController
@RequestMapping("/api/v1")
class SessionReportController {

  private final SessionReportService service;
  private final UserDirectory userDirectory;
  private final Clock clock;

  SessionReportController(SessionReportService service, UserDirectory userDirectory, Clock clock) {
    this.service = service;
    this.userDirectory = userDirectory;
    this.clock = clock;
  }

  record ReportDto(
      UUID id,
      UUID sessionId,
      UUID teacherId,
      UUID studentId,
      SessionReport.Status status,
      Instant sessionEndsAt,
      Instant dueAt,
      boolean overdue,
      Boolean onTime,
      List<UUID> taughtTopicIds,
      SessionReport.Scores scores,
      String comment,
      Instant submittedAt) {

    static ReportDto of(SessionReport r, Instant now) {
      return new ReportDto(
          r.getId(),
          r.getSessionId(),
          r.getTeacherId(),
          r.getStudentId(),
          r.getStatus(),
          r.getSessionEndsAt(),
          r.getDueAt(),
          r.isOverdue(now),
          r.isOnTime(),
          r.getTaughtTopicIds(),
          r.getScores(),
          r.getComment(),
          r.getSubmittedAt());
    }
  }

  record ReportListItem(
      UUID sessionId,
      UUID teacherId,
      String teacherName,
      UUID studentId,
      String studentName,
      SessionReport.Status status,
      Instant sessionEndsAt,
      Instant dueAt,
      boolean overdue,
      Boolean onTime) {}

  record AssignmentRequest(String title, String instructions, Instant dueAt) {}

  record SubmitRequest(
      List<UUID> taughtTopicIds,
      List<UUID> completedTopicIds,
      SessionReport.Scores scores,
      String comment,
      List<AssignmentRequest> assignments) {}

  /** Phiếu của học viên đã ẩn danh không còn việc để làm nên không hiện trong danh sách. */
  @GetMapping("/session-reports")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  List<ReportListItem> list(@RequestParam(required = false) SessionReport.Status status) {
    List<SessionReport> reports = service.list(CurrentUser.require(), status);
    Map<UUID, UserView> people =
        userDirectory
            .findAll(
                reports.stream()
                    .flatMap(r -> List.of(r.getTeacherId(), r.getStudentId()).stream())
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(UserView::id, Function.identity()));
    Instant now = clock.instant();
    return reports.stream()
        .filter(
            r -> {
              UserView student = people.get(r.getStudentId());
              return student != null && !student.anonymized();
            })
        .map(
            r -> {
              UserView teacher = people.get(r.getTeacherId());
              return new ReportListItem(
                  r.getSessionId(),
                  r.getTeacherId(),
                  teacher == null ? null : teacher.fullName(),
                  r.getStudentId(),
                  people.get(r.getStudentId()).fullName(),
                  r.getStatus(),
                  r.getSessionEndsAt(),
                  r.getDueAt(),
                  r.isOverdue(now),
                  r.isOnTime());
            })
        .toList();
  }

  @GetMapping("/sessions/{sessionId}/report")
  @PreAuthorize("isAuthenticated()")
  ReportDto get(@PathVariable UUID sessionId) {
    return ReportDto.of(service.get(CurrentUser.require(), sessionId), clock.instant());
  }

  @PutMapping("/sessions/{sessionId}/report")
  @PreAuthorize("hasRole('TEACHER')")
  ReportDto submit(@PathVariable UUID sessionId, @RequestBody SubmitRequest r) {
    SessionReportService.Submission input =
        new SessionReportService.Submission(
            r.taughtTopicIds() == null ? List.of() : r.taughtTopicIds(),
            r.completedTopicIds() == null ? List.of() : r.completedTopicIds(),
            r.scores(),
            r.comment(),
            r.assignments() == null
                ? List.of()
                : r.assignments().stream()
                    .map(
                        a ->
                            a == null
                                ? new SessionReportService.NewAssignment(null, null, null)
                                : new SessionReportService.NewAssignment(
                                    a.title(), a.instructions(), a.dueAt()))
                    .toList());
    return ReportDto.of(service.submit(CurrentUser.require(), sessionId, input), clock.instant());
  }
}
