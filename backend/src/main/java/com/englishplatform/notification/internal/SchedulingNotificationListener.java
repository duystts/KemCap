package com.englishplatform.notification.internal;

import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.scheduling.SchedulingEvents.CancelCause;
import com.englishplatform.scheduling.SchedulingEvents.ReminderWindow;
import com.englishplatform.scheduling.SchedulingEvents.SessionBooked;
import com.englishplatform.scheduling.SchedulingEvents.SessionCancelled;
import com.englishplatform.scheduling.SchedulingEvents.SessionOutcomeOverdue;
import com.englishplatform.scheduling.SchedulingEvents.SessionReminderDue;
import com.englishplatform.scheduling.SchedulingEvents.SessionRescheduled;
import com.englishplatform.scheduling.SessionQueryApi;
import com.englishplatform.scheduling.SessionQueryApi.SessionView;
import com.englishplatform.scheduling.SessionStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Thông báo cho event lịch học (T132, FR-013, FR-014). Giờ hiển thị theo múi giờ từng người nhận;
 * {@code dedupe_key} theo buổi + người nhận nên event phát lại không gửi trùng (NFR-REL-05).
 */
@Component
class SchedulingNotificationListener {

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
  void on(SessionBooked event) {
    UserView student = userDirectory.find(event.studentId()).orElse(null);
    UserView teacher = userDirectory.find(event.teacherId()).orElse(null);
    if (student == null || teacher == null) {
      return;
    }
    String link = sessionLink(event.sessionId());
    for (Party p : List.of(Party.of(student, teacher, null), Party.of(teacher, student, student))) {
      String time = NotificationFormat.dateTime(event.startsAt(), p.me().timezone());
      Map<String, Object> model = new HashMap<>();
      model.put("fullName", p.me().fullName());
      model.put("otherPartyRole", p.otherRole());
      model.put("otherPartyName", p.other().fullName());
      model.put("formattedTime", time);
      model.put("link", link);
      dispatchService.notify(
          "session:" + event.sessionId() + ":booked:" + p.me().id(),
          p.me().id(),
          p.me().email(),
          "Xác nhận buổi học lúc " + time,
          "session-booked",
          model,
          new DispatchService.InApp(
              "SESSION_BOOKED",
              "Buổi học mới đã được đặt",
              "Buổi học với " + p.other().fullName() + " lúc " + time,
              link,
              p.aboutStudentId()));
    }
  }

  /**
   * Báo bên còn lại: HV hủy → báo GV; GV hủy → báo HV; admin/hệ thống hủy → báo cả hai. {@code
   * TEACHER_ABSENT} báo thêm mọi admin. Hủy do đổi GV đã được gộp ở {@code
   * ReassignmentSessionsCancelled}; hủy do ẩn danh hóa chỉ báo GV.
   */
  @ApplicationModuleListener
  void on(SessionCancelled event) {
    if (event.cause() == CancelCause.TEACHER_REASSIGNED) {
      return;
    }
    SessionView session = sessionQueryApi.find(event.sessionId()).orElse(null);
    if (session == null) {
      return;
    }
    UserView student = userDirectory.find(session.studentId()).orElse(null);
    UserView teacher = userDirectory.find(session.teacherId()).orElse(null);
    if (student == null || teacher == null) {
      return;
    }
    boolean byStudent = student.id().equals(event.cancelledBy());
    boolean byTeacher = teacher.id().equals(event.cancelledBy());
    boolean notifyStudent = !byStudent && event.cause() != CancelCause.ANONYMIZED;
    boolean notifyTeacher = !byTeacher;

    String reason = reasonText(event.status(), event.cause());
    String link = sessionLink(event.sessionId());
    if (notifyStudent) {
      String consumeNote =
          event.status() == SessionStatus.CANCELLED_LATE
              ? "Buổi học này bị trừ vào gói do hủy sau hạn chót."
              : null;
      sendCancelled(event, student, reason, consumeNote, link, null, "SESSION_CANCELLED");
    }
    if (notifyTeacher) {
      // ẩn danh hóa: nội dung không nêu tên HV và không gắn HV (listener dọn dữ liệu có thể chạy
      // trước)
      UUID about = event.cause() == CancelCause.ANONYMIZED ? null : student.id();
      sendCancelled(event, teacher, reason, null, link, about, "SESSION_CANCELLED");
    }
    if (event.status() == SessionStatus.TEACHER_ABSENT) {
      String adminReason =
          "Giáo viên " + teacher.fullName() + " vắng/hủy buổi với học viên " + student.fullName();
      for (UserView admin : userDirectory.findAdmins()) {
        sendCancelled(
            event,
            admin,
            adminReason,
            "Cần xếp buổi bù hoặc đổi giáo viên cho học viên.",
            link,
            student.id(),
            "TEACHER_ABSENT");
      }
    }
  }

  @ApplicationModuleListener
  void on(SessionRescheduled event) {
    SessionView session = sessionQueryApi.find(event.newSessionId()).orElse(null);
    if (session == null) {
      return;
    }
    UserView student = userDirectory.find(session.studentId()).orElse(null);
    UserView teacher = userDirectory.find(session.teacherId()).orElse(null);
    if (student == null || teacher == null) {
      return;
    }
    String link = sessionLink(event.newSessionId());
    for (Party p : List.of(Party.of(student, teacher, null), Party.of(teacher, student, student))) {
      String time = NotificationFormat.dateTime(event.newStartsAt(), p.me().timezone());
      Map<String, Object> model = new HashMap<>();
      model.put("fullName", p.me().fullName());
      model.put("formattedTime", time);
      model.put("link", link);
      dispatchService.notify(
          "session:" + event.newSessionId() + ":rescheduled:" + p.me().id(),
          p.me().id(),
          p.me().email(),
          "Buổi học đã đổi sang " + time,
          "session-rescheduled",
          model,
          new DispatchService.InApp(
              "SESSION_RESCHEDULED",
              "Buổi học đã đổi lịch",
              "Buổi học với " + p.other().fullName() + " chuyển sang " + time,
              link,
              p.aboutStudentId()));
    }
  }

  /** Dedupe {@code session:{id}:reminder:{window}:{userId}} (T132). */
  @ApplicationModuleListener
  void on(SessionReminderDue event) {
    UserView student = userDirectory.find(event.studentId()).orElse(null);
    UserView teacher = userDirectory.find(event.teacherId()).orElse(null);
    if (student == null || teacher == null) {
      return;
    }
    String windowText = event.window() == ReminderWindow.H24 ? "24 giờ" : "1 giờ";
    String link = sessionLink(event.sessionId());
    for (Party p : List.of(Party.of(student, teacher, null), Party.of(teacher, student, student))) {
      String time = NotificationFormat.dateTime(event.startsAt(), p.me().timezone());
      Map<String, Object> model = new HashMap<>();
      model.put("fullName", p.me().fullName());
      model.put("windowText", windowText);
      model.put("formattedTime", time);
      model.put("otherPartyRole", p.otherRole());
      model.put("otherPartyName", p.other().fullName());
      model.put("link", link);
      dispatchService.notify(
          "session:" + event.sessionId() + ":reminder:" + event.window() + ":" + p.me().id(),
          p.me().id(),
          p.me().email(),
          "Nhắc lịch: buổi học lúc " + time + " (còn " + windowText + ")",
          "session-reminder",
          model,
          new DispatchService.InApp(
              "SESSION_REMINDER",
              "Buổi học sau " + windowText,
              "Buổi học với " + p.other().fullName() + " lúc " + time,
              link,
              p.aboutStudentId()));
    }
  }

  /** FR-014: sau 24h chưa chốt kết quả → nhắc giáo viên và mọi admin. */
  @ApplicationModuleListener
  void on(SessionOutcomeOverdue event) {
    UserView teacher = userDirectory.find(event.teacherId()).orElse(null);
    if (teacher == null) {
      return;
    }
    UUID studentId =
        sessionQueryApi.find(event.sessionId()).map(SessionView::studentId).orElse(null);
    String link = sessionLink(event.sessionId());
    List<UserView> recipients = new ArrayList<>();
    recipients.add(teacher);
    recipients.addAll(userDirectory.findAdmins());
    for (UserView r : recipients) {
      boolean isTeacher = r.id().equals(teacher.id());
      String time = NotificationFormat.dateTime(event.endsAt(), r.timezone());
      Map<String, Object> model = new HashMap<>();
      model.put("fullName", r.fullName());
      model.put("formattedTime", time);
      model.put("link", link);
      dispatchService.notify(
          "session:" + event.sessionId() + ":outcome-overdue:" + r.id(),
          r.id(),
          r.email(),
          isTeacher
              ? "Nhắc chốt kết quả buổi học kết thúc lúc " + time
              : "Buổi của giáo viên " + teacher.fullName() + " chưa chốt kết quả sau 24 giờ",
          "session-outcome-overdue",
          model,
          new DispatchService.InApp(
              "OUTCOME_OVERDUE",
              "Buổi học chưa chốt kết quả",
              isTeacher
                  ? "Buổi học kết thúc lúc " + time + " cần được chốt kết quả."
                  : "GV " + teacher.fullName() + " chưa chốt buổi kết thúc lúc " + time,
              link,
              studentId));
    }
  }

  private void sendCancelled(
      SessionCancelled event,
      UserView to,
      String reason,
      String consumeNote,
      String link,
      UUID aboutStudentId,
      String type) {
    String time = NotificationFormat.dateTime(event.startsAt(), to.timezone());
    Map<String, Object> model = new HashMap<>();
    model.put("fullName", to.fullName());
    model.put("formattedTime", time);
    model.put("reason", reason);
    model.put("consumeNote", consumeNote);
    model.put("link", link);
    dispatchService.notify(
        "session:" + event.sessionId() + ":cancelled:" + to.id(),
        to.id(),
        to.email(),
        "Buổi học lúc " + time + " đã bị hủy",
        "session-cancelled",
        model,
        new DispatchService.InApp(
            type,
            "Buổi học đã bị hủy",
            "Buổi học lúc " + time + " đã bị hủy (" + reason + ")",
            link,
            aboutStudentId));
  }

  private static String reasonText(SessionStatus status, CancelCause cause) {
    return switch (status) {
      case CANCELLED_ON_TIME -> "Hủy trước hạn chót";
      case CANCELLED_LATE -> "Hủy sau hạn chót";
      case TEACHER_ABSENT -> "Giáo viên vắng/hủy";
      case CANCELLED_SYSTEM ->
          Objects.equals(cause, CancelCause.USER_DEACTIVATED)
              ? "Tài khoản học viên ngừng hoạt động"
              : "Hệ thống hủy tự động";
      default -> "Buổi học bị hủy";
    };
  }

  private String sessionLink(UUID sessionId) {
    return frontendUrl + "/sessions/" + sessionId;
  }

  /** Người nhận {@code me} và bên kia; {@code aboutStudentId} khác null khi người nhận là GV. */
  private record Party(UserView me, UserView other, String otherRole, UUID aboutStudentId) {

    static Party of(UserView me, UserView other, UserView studentMentioned) {
      return new Party(
          me,
          other,
          studentMentioned == null ? "Giáo viên" : "Học viên",
          studentMentioned == null ? null : studentMentioned.id());
    }
  }
}
