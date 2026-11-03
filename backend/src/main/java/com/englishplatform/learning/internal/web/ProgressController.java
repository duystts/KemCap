package com.englishplatform.learning.internal.web;

import com.englishplatform.learning.internal.progress.ProgressService;
import com.englishplatform.learning.internal.report.SessionReport;
import com.englishplatform.shared.security.CurrentUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Trang tiến bộ (FR-021). Không có quyền xem → 404. */
@RestController
class ProgressController {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final ProgressService service;

  ProgressController(ProgressService service) {
    this.service = service;
  }

  record SkillPoint(
      UUID sessionId,
      LocalDate date,
      Integer listening,
      Integer speaking,
      Integer reading,
      Integer writing,
      Integer grammarVocab) {

    static SkillPoint of(SessionReport r) {
      SessionReport.Scores s = r.getScores();
      return new SkillPoint(
          r.getSessionId(),
          r.getSessionEndsAt().atZone(BUSINESS_ZONE).toLocalDate(),
          s.listening(),
          s.speaking(),
          s.reading(),
          s.writing(),
          s.grammarVocab());
    }
  }

  record RecentReport(
      UUID sessionId,
      Instant sessionEndsAt,
      Instant submittedAt,
      SessionReport.Scores scores,
      String comment) {}

  record ProgressDto(
      List<SkillPoint> skillSeries, Integer pathCompletion, List<RecentReport> recentReports) {}

  @GetMapping("/api/v1/students/{studentId}/progress")
  @PreAuthorize("isAuthenticated()")
  ProgressDto progress(@PathVariable UUID studentId) {
    ProgressService.Progress p = service.progress(CurrentUser.require(), studentId);
    return new ProgressDto(
        p.reports().stream().map(SkillPoint::of).toList(),
        p.pathCompletion(),
        p.recentReports().stream()
            .map(
                r ->
                    new RecentReport(
                        r.getSessionId(),
                        r.getSessionEndsAt(),
                        r.getSubmittedAt(),
                        r.getScores(),
                        r.getComment()))
            .toList());
  }
}
