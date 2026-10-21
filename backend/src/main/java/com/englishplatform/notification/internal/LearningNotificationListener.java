package com.englishplatform.notification.internal;

import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.learning.LearningEvents;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Thông báo phiếu đánh giá và bài tập (T134, FR-019); event do U6 phát. */
@Component
class LearningNotificationListener {

  private final DispatchService dispatchService;
  private final UserDirectory userDirectory;
  private final String frontendUrl;

  LearningNotificationListener(
      DispatchService dispatchService,
      UserDirectory userDirectory,
      @Value("${app.frontend-url}") String frontendUrl) {
    this.dispatchService = dispatchService;
    this.userDirectory = userDirectory;
    this.frontendUrl = frontendUrl;
  }

  @ApplicationModuleListener
  void on(LearningEvents.SessionReportOverdue event) {
    userDirectory
        .find(event.teacherId())
        .ifPresent(
            teacher -> {
              String link = frontendUrl + "/sessions/" + event.sessionId();
              Map<String, Object> model = new HashMap<>();
              model.put("fullName", teacher.fullName());
              model.put("link", link);

              dispatchService.notify(
                  "learning:report-overdue:" + event.sessionId() + ":" + teacher.id(),
                  teacher.id(),
                  teacher.email(),
                  "Nhắc nhở nộp phiếu đánh giá sau buổi học",
                  "session-report-overdue",
                  model,
                  new DispatchService.InApp(
                      "REPORT_OVERDUE",
                      "Nhắc nộp phiếu đánh giá",
                      "Buổi học đã kết thúc hơn 24h nhưng chưa có phiếu nhận xét.",
                      link));
            });
  }

  @ApplicationModuleListener
  void on(LearningEvents.SessionReportSubmitted event) {
    userDirectory
        .find(event.studentId())
        .ifPresent(
            student -> {
              String link = frontendUrl + "/sessions/" + event.sessionId();
              Map<String, Object> model = new HashMap<>();
              model.put("fullName", student.fullName());
              model.put("link", link);

              dispatchService.notify(
                  "learning:report-submitted:" + event.sessionId(),
                  student.id(),
                  student.email(),
                  "Đã có nhận xét cho buổi học vừa qua",
                  "session-report-submitted",
                  model,
                  new DispatchService.InApp(
                      "REPORT_SUBMITTED",
                      "Đã có nhận xét buổi học",
                      "Giáo viên đã gửi nhận xét và đánh giá kỹ năng cho buổi học của bạn.",
                      link));
            });
  }

  @ApplicationModuleListener
  void on(LearningEvents.AssignmentSubmitted event) {
    UserView teacher = userDirectory.find(event.teacherId()).orElse(null);
    UserView student = userDirectory.find(event.studentId()).orElse(null);
    if (teacher == null || student == null) return;

    String link = frontendUrl + "/teacher/students/" + student.id();
    Map<String, Object> model = new HashMap<>();
    model.put("fullName", teacher.fullName());
    model.put("studentName", student.fullName());
    model.put("link", link);

    dispatchService.notify(
        "learning:assignment-submitted:" + event.assignmentId(),
        teacher.id(),
        teacher.email(),
        "Học viên " + student.fullName() + " đã nộp bài tập",
        "assignment-submitted",
        model,
        new DispatchService.InApp(
            "ASSIGNMENT_SUBMITTED",
            "Bài tập mới cần chấm",
            "Học viên " + student.fullName() + " vừa nộp bài tập.",
            link,
            student.id()));
  }

  @ApplicationModuleListener
  void on(LearningEvents.AssignmentGraded event) {
    userDirectory
        .find(event.studentId())
        .ifPresent(
            student -> {
              String link = frontendUrl + "/student/progress";
              Map<String, Object> model = new HashMap<>();
              model.put("fullName", student.fullName());
              model.put("score", event.score());
              model.put("link", link);

              dispatchService.notify(
                  "learning:assignment-graded:" + event.assignmentId(),
                  student.id(),
                  student.email(),
                  "Bài tập của bạn đã được chấm điểm (" + event.score() + "/10)",
                  "assignment-graded",
                  model,
                  new DispatchService.InApp(
                      "ASSIGNMENT_GRADED",
                      "Bài tập đã chấm điểm",
                      "Bài tập của bạn đạt " + event.score() + "/10 điểm. Bấm để xem nhận xét.",
                      link));
            });
  }
}
