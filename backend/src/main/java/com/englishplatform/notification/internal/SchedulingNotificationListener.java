package com.englishplatform.notification.internal;

import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.scheduling.SchedulingEvents;
import com.englishplatform.scheduling.SessionQueryApi;
import com.englishplatform.scheduling.SessionStatus;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Xử lý thông báo cho các sự kiện lịch học (T132). */
@Component
class SchedulingNotificationListener {

  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

  private final DispatchService dispatchService;
  private final UserDirectory userDirectory;
  private final SessionQueryApi sessionQueryApi;
  private final String frontendUrl;

  SchedulingNotificationListener(
      DispatchService dispatchService,
      UserDirectory userDirectory,
      SessionQueryApi sessionQueryApi,
      @Value("${app.frontend-url}") String frontendUrl) {
    this.dispatchService = dispatchService;
    this.userDirectory = userDirectory;
    this.sessionQueryApi = sessionQueryApi;
    this.frontendUrl = frontendUrl;
  }

  @ApplicationModuleListener
  void on(SchedulingEvents.SessionBooked event) {
    userDirectory
        .find(event.studentId())
        .ifPresent(
            student -> {
              userDirectory
                  .find(event.teacherId())
                  .ifPresent(
                      teacher -> {
                        // 1. Gửi cho học viên
                        String studentTime = formatInstant(event.startsAt(), student.timezone());
                        String studentLink = frontendUrl + "/student/schedule";
                        Map<String, Object> studentModel = new HashMap<>();
                        studentModel.put("fullName", student.fullName());
                        studentModel.put("otherPartyRole", "Giáo viên");
                        studentModel.put("otherPartyName", teacher.fullName());
                        studentModel.put("formattedTime", studentTime);
                        studentModel.put("link", studentLink);

                        dispatchService.notify(
                            "session:" + event.sessionId() + ":booked:student:" + event.studentId(),
                            event.studentId(),
                            student.email(),
                            "Xác nhận lịch học: " + studentTime,
                            "session-booked",
                            studentModel,
                            new DispatchService.InApp(
                                "SESSION_BOOKED",
                                "Đặt lịch thành công",
                                "Buổi học với " + teacher.fullName() + " lúc " + studentTime,
                                studentLink));

                        // 2. Gửi cho giáo viên
                        String teacherTime = formatInstant(event.startsAt(), teacher.timezone());
                        String teacherLink = frontendUrl + "/teacher/schedule";
                        Map<String, Object> teacherModel = new HashMap<>();
                        teacherModel.put("fullName", teacher.fullName());
                        teacherModel.put("otherPartyRole", "Học viên");
                        teacherModel.put("otherPartyName", student.fullName());
                        teacherModel.put("formattedTime", teacherTime);
                        teacherModel.put("link", teacherLink);

                        dispatchService.notify(
                            "session:" + event.sessionId() + ":booked:teacher:" + event.teacherId(),
                            event.teacherId(),
                            teacher.email(),
                            "Có lịch dạy mới: " + teacherTime,
                            "session-booked",
                            teacherModel,
                            new DispatchService.InApp(
                                "SESSION_BOOKED",
                                "Có lịch dạy mới",
                                "Học viên "
                                    + student.fullName()
                                    + " đã đặt buổi học lúc "
                                    + teacherTime,
                                teacherLink));
                      });
            });
  }

  @ApplicationModuleListener
  void on(SchedulingEvents.SessionCancelled event) {
    sessionQueryApi
        .find(event.sessionId())
        .ifPresent(
            session -> {
              UserView student = userDirectory.find(session.studentId()).orElse(null);
              UserView teacher = userDirectory.find(session.teacherId()).orElse(null);
              if (student == null || teacher == null) return;

              String studentTime = formatInstant(event.startsAt(), student.timezone());
              String teacherTime = formatInstant(event.startsAt(), teacher.timezone());
              SessionStatus status = event.status();

              String reasonText =
                  switch (status) {
                    case CANCELLED_ON_TIME -> "Học viên hủy đúng hạn";
                    case CANCELLED_LATE -> "Học viên hủy muộn";
                    case TEACHER_ABSENT -> "Giáo viên vắng / hủy";
                    case CANCELLED_SYSTEM -> "Hệ thống tự động hủy";
                    case STUDENT_NO_SHOW -> "Học viên không đến";
                    default -> "Buổi học bị hủy";
                  };

              String consumeNote =
                  (status == SessionStatus.CANCELLED_LATE)
                      ? "Buổi học này bị trừ vào gói do hủy sau hạn chót."
                      : null;

              // Báo học viên
              Map<String, Object> studentModel = new HashMap<>();
              studentModel.put("fullName", student.fullName());
              studentModel.put("formattedTime", studentTime);
              studentModel.put("reason", reasonText);
              studentModel.put("consumeNote", consumeNote);
              studentModel.put("link", frontendUrl + "/student/schedule");

              dispatchService.notify(
                  "session:" + event.sessionId() + ":cancelled:student:" + status,
                  student.id(),
                  student.email(),
                  "Thông báo hủy buổi học lúc " + studentTime,
                  "session-cancelled",
                  studentModel,
                  new DispatchService.InApp(
                      "SESSION_CANCELLED",
                      "Buổi học đã bị hủy",
                      "Buổi học lúc " + studentTime + " đã bị hủy (" + reasonText + ")",
                      frontendUrl + "/student/schedule"));

              // Báo giáo viên
              Map<String, Object> teacherModel = new HashMap<>();
              teacherModel.put("fullName", teacher.fullName());
              teacherModel.put("formattedTime", teacherTime);
              teacherModel.put("reason", reasonText);
              teacherModel.put("consumeNote", null);
              teacherModel.put("link", frontendUrl + "/teacher/schedule");

              dispatchService.notify(
                  "session:" + event.sessionId() + ":cancelled:teacher:" + status,
                  teacher.id(),
                  teacher.email(),
                  "Thông báo hủy buổi học lúc " + teacherTime,
                  "session-cancelled",
                  teacherModel,
                  new DispatchService.InApp(
                      "SESSION_CANCELLED",
                      "Buổi học đã bị hủy",
                      "Buổi học lúc " + teacherTime + " đã bị hủy (" + reasonText + ")",
                      frontendUrl + "/teacher/schedule"));

              // Nếu TEACHER_ABSENT: Báo thêm toàn bộ Admin
              if (status == SessionStatus.TEACHER_ABSENT) {
                for (UserView admin : userDirectory.findAdmins()) {
                  Map<String, Object> adminModel = new HashMap<>();
                  adminModel.put("fullName", admin.fullName());
                  adminModel.put(
                      "formattedTime", formatInstant(event.startsAt(), admin.timezone()));
                  adminModel.put(
                      "reason",
                      "Giáo viên "
                          + teacher.fullName()
                          + " vắng/hủy buổi học với học viên "
                          + student.fullName());
                  adminModel.put("consumeNote", "Cần xem xét xếp buổi bù hoặc hỗ trợ học viên.");
                  adminModel.put("link", frontendUrl + "/admin/schedule");

                  dispatchService.notify(
                      "session:" + event.sessionId() + ":cancelled:admin:" + admin.id(),
                      admin.id(),
                      admin.email(),
                      "[Cảnh báo] Giáo viên vắng buổi học lúc " + teacherTime,
                      "session-cancelled",
                      adminModel,
                      new DispatchService.InApp(
                          "TEACHER_ABSENT",
                          "Giáo viên vắng buổi học",
                          "GV "
                              + teacher.fullName()
                              + " vắng buổi học với HV "
                              + student.fullName(),
                          frontendUrl + "/admin/schedule"));
                }
              }
            });
  }

  @ApplicationModuleListener
  void on(SchedulingEvents.SessionRescheduled event) {
    sessionQueryApi
        .find(event.newSessionId())
        .ifPresent(
            session -> {
              UserView student = userDirectory.find(session.studentId()).orElse(null);
              UserView teacher = userDirectory.find(session.teacherId()).orElse(null);
              if (student == null || teacher == null) return;

              // Báo học viên
              String studentTime = formatInstant(event.newStartsAt(), student.timezone());
              Map<String, Object> studentModel = new HashMap<>();
              studentModel.put("fullName", student.fullName());
              studentModel.put("formattedTime", studentTime);
              studentModel.put("link", frontendUrl + "/student/schedule");

              dispatchService.notify(
                  "session:" + event.newSessionId() + ":rescheduled:student",
                  student.id(),
                  student.email(),
                  "Đổi lịch học thành công: " + studentTime,
                  "session-rescheduled",
                  studentModel,
                  new DispatchService.InApp(
                      "SESSION_RESCHEDULED",
                      "Đổi lịch học thành công",
                      "Buổi học đã chuyển sang " + studentTime,
                      frontendUrl + "/student/schedule"));

              // Báo giáo viên
              String teacherTime = formatInstant(event.newStartsAt(), teacher.timezone());
              Map<String, Object> teacherModel = new HashMap<>();
              teacherModel.put("fullName", teacher.fullName());
              teacherModel.put("formattedTime", teacherTime);
              teacherModel.put("link", frontendUrl + "/teacher/schedule");

              dispatchService.notify(
                  "session:" + event.newSessionId() + ":rescheduled:teacher",
                  teacher.id(),
                  teacher.email(),
                  "Buổi học đã đổi lịch: " + teacherTime,
                  "session-rescheduled",
                  teacherModel,
                  new DispatchService.InApp(
                      "SESSION_RESCHEDULED",
                      "Buổi học đã đổi lịch",
                      "Học viên " + student.fullName() + " đã đổi lịch sang " + teacherTime,
                      frontendUrl + "/teacher/schedule"));
            });
  }

  @ApplicationModuleListener
  void on(SchedulingEvents.SessionReminderDue event) {
    UserView student = userDirectory.find(event.studentId()).orElse(null);
    UserView teacher = userDirectory.find(event.teacherId()).orElse(null);
    if (student == null || teacher == null) return;

    String windowText =
        (event.window() == SchedulingEvents.ReminderWindow.H24) ? "24 giờ" : "1 giờ";
    String windowCode = event.window().name();

    // Nhắc học viên
    String studentTime = formatInstant(event.startsAt(), student.timezone());
    String studentLink = frontendUrl + "/student/schedule";
    Map<String, Object> studentModel = new HashMap<>();
    studentModel.put("fullName", student.fullName());
    studentModel.put("windowText", windowText);
    studentModel.put("formattedTime", studentTime);
    studentModel.put("otherPartyRole", "Giáo viên");
    studentModel.put("otherPartyName", teacher.fullName());
    studentModel.put("link", studentLink);

    dispatchService.notify(
        "session:" + event.sessionId() + ":reminder:" + windowCode + ":" + student.id(),
        student.id(),
        student.email(),
        "Nhắc lịch học: Buổi học diễn ra trong " + windowText + " tới",
        "session-reminder",
        studentModel,
        new DispatchService.InApp(
            "SESSION_REMINDER",
            "Nhắc lịch học (" + windowText + ")",
            "Buổi học với " + teacher.fullName() + " lúc " + studentTime,
            studentLink));

    // Nhắc giáo viên
    String teacherTime = formatInstant(event.startsAt(), teacher.timezone());
    String teacherLink = frontendUrl + "/teacher/schedule";
    Map<String, Object> teacherModel = new HashMap<>();
    teacherModel.put("fullName", teacher.fullName());
    teacherModel.put("windowText", windowText);
    teacherModel.put("formattedTime", teacherTime);
    teacherModel.put("otherPartyRole", "Học viên");
    teacherModel.put("otherPartyName", student.fullName());
    teacherModel.put("link", teacherLink);

    dispatchService.notify(
        "session:" + event.sessionId() + ":reminder:" + windowCode + ":" + teacher.id(),
        teacher.id(),
        teacher.email(),
        "Nhắc lịch dạy: Buổi học diễn ra trong " + windowText + " tới",
        "session-reminder",
        teacherModel,
        new DispatchService.InApp(
            "SESSION_REMINDER",
            "Nhắc lịch dạy (" + windowText + ")",
            "Buổi dạy với " + student.fullName() + " lúc " + teacherTime,
            teacherLink));
  }

  @ApplicationModuleListener
  void on(SchedulingEvents.SessionOutcomeOverdue event) {
    userDirectory
        .find(event.teacherId())
        .ifPresent(
            teacher -> {
              String teacherTime = formatInstant(event.endsAt(), teacher.timezone());
              String teacherLink = frontendUrl + "/teacher/schedule";

              Map<String, Object> teacherModel = new HashMap<>();
              teacherModel.put("fullName", teacher.fullName());
              teacherModel.put("formattedTime", teacherTime);
              teacherModel.put("link", teacherLink);

              dispatchService.notify(
                  "session:" + event.sessionId() + ":outcome-overdue:" + teacher.id(),
                  teacher.id(),
                  teacher.email(),
                  "Nhắc nhở: Buổi học kết thúc lúc " + teacherTime + " chưa chốt kết quả",
                  "session-outcome-overdue",
                  teacherModel,
                  new DispatchService.InApp(
                      "OUTCOME_OVERDUE",
                      "Chưa chốt kết quả buổi học",
                      "Buổi học kết thúc lúc " + teacherTime + " cần được chốt kết quả.",
                      teacherLink));

              // Báo admin
              for (UserView admin : userDirectory.findAdmins()) {
                Map<String, Object> adminModel = new HashMap<>();
                adminModel.put("fullName", admin.fullName());
                adminModel.put("formattedTime", formatInstant(event.endsAt(), admin.timezone()));
                adminModel.put("link", frontendUrl + "/admin/schedule");

                dispatchService.notify(
                    "session:" + event.sessionId() + ":outcome-overdue:admin:" + admin.id(),
                    admin.id(),
                    admin.email(),
                    "[Cảnh báo] Buổi dạy của " + teacher.fullName() + " chưa chốt kết quả sau 24h",
                    "session-outcome-overdue",
                    adminModel,
                    new DispatchService.InApp(
                        "OUTCOME_OVERDUE",
                        "Buổi học chưa chốt kết quả",
                        "GV "
                            + teacher.fullName()
                            + " chưa chốt kết quả buổi học kết thúc lúc "
                            + teacherTime,
                        frontendUrl + "/admin/schedule"));
              }
            });
  }

  private static String formatInstant(Instant instant, String timezone) {
    if (instant == null) return "";
    ZoneId zoneId;
    try {
      zoneId =
          (timezone != null && !timezone.isBlank())
              ? ZoneId.of(timezone)
              : ZoneId.of("Asia/Ho_Chi_Minh");
    } catch (Exception e) {
      zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
    }
    return instant.atZone(zoneId).format(TIME_FORMATTER);
  }
}
