package com.englishplatform.learning.internal.progress;

import com.englishplatform.learning.internal.StudentAccessGuard;
import com.englishplatform.learning.internal.path.LearningPath;
import com.englishplatform.learning.internal.path.LearningPathRepository;
import com.englishplatform.learning.internal.report.SessionReport;
import com.englishplatform.learning.internal.report.SessionReportRepository;
import com.englishplatform.shared.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trang tiến bộ (FR-021): diễn biến điểm từ phiếu đã nộp, % chủ đề hoàn thành của lộ trình đang
 * dùng, nhận xét gần nhất. Không có tỷ lệ chuyên cần/nộp bài ([GĐ2]).
 */
@Service
public class ProgressService {

  private static final int RECENT_REPORTS = 5;

  private final SessionReportRepository reports;
  private final LearningPathRepository paths;
  private final StudentAccessGuard guard;

  ProgressService(
      SessionReportRepository reports, LearningPathRepository paths, StudentAccessGuard guard) {
    this.reports = reports;
    this.paths = paths;
    this.guard = guard;
  }

  /** {@code pathCompletion}: phần trăm; null khi chưa có lộ trình hoặc lộ trình chưa có chủ đề. */
  public record Progress(List<SessionReport> reports, Integer pathCompletion) {

    public List<SessionReport> recentReports() {
      int from = Math.max(0, reports.size() - RECENT_REPORTS);
      return reports.subList(from, reports.size()).reversed();
    }
  }

  @Transactional(readOnly = true)
  public Progress progress(CurrentUser actor, UUID studentId) {
    guard.requireRead(actor, studentId);
    return new Progress(reports.findSubmittedByStudent(studentId), pathCompletion(studentId));
  }

  private Integer pathCompletion(UUID studentId) {
    return paths
        .findByStudentIdAndStatus(studentId, LearningPath.Status.ACTIVE)
        .map(
            path -> {
              boolean noTopics =
                  path.getMilestones().stream().allMatch(m -> m.getTopics().isEmpty());
              return noTopics ? null : path.completionPercent();
            })
        .orElse(null);
  }
}
