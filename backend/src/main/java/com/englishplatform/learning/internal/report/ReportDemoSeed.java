package com.englishplatform.learning.internal.report;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.learning.internal.path.LearningPath;
import com.englishplatform.learning.internal.path.LearningPathRepository;
import com.englishplatform.learning.internal.path.Topic;
import com.englishplatform.scheduling.SessionQueryApi;
import com.englishplatform.scheduling.SessionStatus;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu đánh giá demo của student1 (sau seed lộ trình), để trang Tiến bộ có dữ liệu ngay khi demo:
 * điểm tăng dần qua các buổi đã học, một kỹ năng bỏ trống, vài chủ đề đã hoàn thành. Chạy lại không
 * tạo trùng.
 */
@Component
@Profile("demo-seed")
@Order(45)
class ReportDemoSeed implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(ReportDemoSeed.class);

  private static final List<SessionReport.Scores> SCORES =
      List.of(
          new SessionReport.Scores(2, 2, 3, 2, 2),
          new SessionReport.Scores(3, 2, 3, null, 3),
          new SessionReport.Scores(3, 3, 4, 3, 3),
          new SessionReport.Scores(4, 4, 4, 3, 4));
  private static final List<String> COMMENTS =
      List.of(
          "Học viên còn ngại nói, cần luyện phản xạ ở các câu ngắn.",
          "Nghe tốt hơn. Bài viết để buổi sau vì hết giờ.",
          "Đọc hiểu tiến bộ rõ, phát âm cần chú ý âm cuối.",
          "Nói trôi chảy hơn nhiều, tự tin khi kể chuyện. Tiếp tục luyện viết.");

  private final SessionReportRepository reports;
  private final LearningPathRepository paths;
  private final SessionQueryApi sessions;
  private final EnrollmentApi enrollmentApi;
  private final UserDirectory userDirectory;

  ReportDemoSeed(
      SessionReportRepository reports,
      LearningPathRepository paths,
      SessionQueryApi sessions,
      EnrollmentApi enrollmentApi,
      UserDirectory userDirectory) {
    this.reports = reports;
    this.paths = paths;
    this.sessions = sessions;
    this.enrollmentApi = enrollmentApi;
    this.userDirectory = userDirectory;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    // tài khoản demo đã bị ẩn danh hóa (đổi email): không còn gì để seed, không chặn app khởi động
    UserView student = userDirectory.findByEmail("student1@demo.local").orElse(null);
    if (student == null || !reports.findSubmittedByStudent(student.id()).isEmpty()) {
      return;
    }
    List<SessionQueryApi.SessionView> completed =
        enrollmentApi.findByStudent(student.id()).stream()
            .flatMap(e -> sessions.findByEnrollment(e.id()).stream())
            .filter(s -> s.status() == SessionStatus.COMPLETED)
            .sorted(Comparator.comparing(SessionQueryApi.SessionView::endsAt))
            .toList();
    List<Topic> topics =
        paths
            .findByStudentIdAndStatus(student.id(), LearningPath.Status.ACTIVE)
            .map(p -> p.getMilestones().stream().flatMap(m -> m.getTopics().stream()).toList())
            .orElse(List.of());

    int count = Math.min(completed.size(), SCORES.size());
    for (int i = 0; i < count; i++) {
      SessionQueryApi.SessionView session = completed.get(i);
      SessionReport report =
          new SessionReport(
              session.id(),
              session.teacherId(),
              session.studentId(),
              session.endsAt(),
              session.endsAt().plus(Duration.ofHours(24)));
      List<Topic> taught = i < topics.size() ? List.of(topics.get(i)) : List.of();
      taught.forEach(Topic::markDone);
      report.submit(
          SCORES.get(i),
          taught.stream().map(Topic::getId).toList(),
          COMMENTS.get(i),
          session.endsAt().plus(Duration.ofHours(2)));
      reports.save(report);
    }
    log.info("Demo seed: {} phiếu đánh giá của student1", count);
  }
}
