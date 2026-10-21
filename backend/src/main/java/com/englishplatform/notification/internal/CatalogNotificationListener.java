package com.englishplatform.notification.internal;

import com.englishplatform.catalog.CatalogEvents.EnrollmentActivated;
import com.englishplatform.catalog.CatalogEvents.EnrollmentRenewalDue;
import com.englishplatform.catalog.CatalogEvents.RenewalReason;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.scheduling.SchedulingEvents.ReassignmentSessionsCancelled;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Thông báo đổi giáo viên, kích hoạt gói và nhắc gia hạn (T133, FR-006, FR-026). */
@Component
class CatalogNotificationListener {

  private final DispatchService dispatchService;
  private final UserDirectory userDirectory;
  private final String frontendUrl;

  CatalogNotificationListener(
      DispatchService dispatchService,
      UserDirectory userDirectory,
      @Value("${app.frontend-url}") String frontendUrl) {
    this.dispatchService = dispatchService;
    this.userDirectory = userDirectory;
    this.frontendUrl = frontendUrl;
  }

  /**
   * FR-006: một thông báo gộp cho học viên và hai giáo viên, liệt kê các buổi đã hủy (thay cho một
   * email hủy mỗi buổi). Nghe event của scheduling vì chỉ sau đó mới biết buổi nào đã bị hủy.
   */
  @ApplicationModuleListener
  void on(ReassignmentSessionsCancelled event) {
    UserView student = userDirectory.find(event.studentId()).orElse(null);
    UserView from = userDirectory.find(event.fromTeacherId()).orElse(null);
    UserView to = userDirectory.find(event.toTeacherId()).orElse(null);
    if (student == null || from == null || to == null) {
      return;
    }
    // mỗi lần đổi một khóa: đổi A→B→A→B vẫn báo đủ, phát lại event không gửi trùng
    String key =
        "enrollment:" + event.enrollmentId() + ":reassigned:" + event.reassignmentId() + ":";

    sendReassigned(
        key + student.id(),
        student,
        event,
        "Giáo viên phụ trách của bạn đổi từ " + from.fullName() + " sang " + to.fullName() + ".",
        "Đổi giáo viên phụ trách",
        "Giáo viên phụ trách mới của bạn là " + to.fullName(),
        frontendUrl + "/student/book",
        true,
        null);
    sendReassigned(
        key + from.id(),
        from,
        event,
        "Học viên " + student.fullName() + " đã chuyển sang giáo viên " + to.fullName() + ".",
        "Bàn giao học viên",
        "Học viên " + student.fullName() + " đã chuyển sang GV " + to.fullName(),
        frontendUrl + "/teacher/students",
        false,
        student.id());
    sendReassigned(
        key + to.id(),
        to,
        event,
        "Bạn được phân công phụ trách học viên " + student.fullName() + ".",
        "Học viên mới được phân công",
        "Bạn được phân công phụ trách học viên " + student.fullName(),
        frontendUrl + "/teacher/students/" + student.id(),
        false,
        student.id());
  }

  @ApplicationModuleListener
  void on(EnrollmentActivated event) {
    UserView student = userDirectory.find(event.studentId()).orElse(null);
    if (student == null) {
      return;
    }
    String expiresAt = NotificationFormat.date(event.expiresAt(), student.timezone());
    String link = frontendUrl + "/student/packages";
    Map<String, Object> model = new HashMap<>();
    model.put("fullName", student.fullName());
    model.put("formattedExpiresAt", expiresAt);
    model.put("link", link);
    dispatchService.notify(
        "enrollment:" + event.enrollmentId() + ":activated",
        student.id(),
        student.email(),
        "Gói học của bạn đã được kích hoạt",
        "enrollment-activated",
        model,
        new DispatchService.InApp(
            "ENROLLMENT_ACTIVATED",
            "Gói học đã kích hoạt",
            "Gói học đã sẵn sàng, hạn dùng đến " + expiresAt,
            link));
  }

  /** FR-026: email + thông báo cho học viên và mọi admin; mỗi lý do một lần cho mỗi ghi danh. */
  @ApplicationModuleListener
  void on(EnrollmentRenewalDue event) {
    UserView student = userDirectory.find(event.studentId()).orElse(null);
    if (student == null) {
      return;
    }
    String key = "enrollment:" + event.enrollmentId() + ":renewal:" + event.reason() + ":";

    String studentReason = reasonText(event, student.timezone());
    String studentLink = frontendUrl + "/student/packages";
    dispatchService.notify(
        key + student.id(),
        student.id(),
        student.email(),
        "Nhắc gia hạn gói học",
        "renewal-reminder",
        renewalModel(student, studentReason, event, studentLink),
        new DispatchService.InApp(
            "RENEWAL_DUE", "Nhắc gia hạn gói học", studentReason, studentLink));

    String adminLink = frontendUrl + "/admin/enrollments/" + event.enrollmentId();
    for (UserView admin : userDirectory.findAdmins()) {
      String reason = "Học viên " + student.fullName() + ": " + reasonText(event, admin.timezone());
      dispatchService.notify(
          key + admin.id(),
          admin.id(),
          admin.email(),
          "Học viên " + student.fullName() + " cần gia hạn gói học",
          "renewal-reminder",
          renewalModel(admin, reason, event, adminLink),
          new DispatchService.InApp(
              "RENEWAL_DUE", "Học viên cần gia hạn", reason, adminLink, student.id()));
    }
  }

  private void sendReassigned(
      String dedupeKey,
      UserView to,
      ReassignmentSessionsCancelled event,
      String message,
      String title,
      String body,
      String link,
      boolean rebookHint,
      UUID aboutStudentId) {
    List<String> times =
        event.cancelledStartsAt().stream()
            .map(t -> NotificationFormat.dateTime(t, to.timezone()))
            .toList();
    Map<String, Object> model = new HashMap<>();
    model.put("fullName", to.fullName());
    model.put("message", message);
    model.put("cancelledTimes", times);
    model.put("rebookHint", rebookHint);
    model.put("link", link);
    String suffix = times.isEmpty() ? "" : " (" + times.size() + " buổi đã đặt được hủy)";
    dispatchService.notify(
        dedupeKey,
        to.id(),
        to.email(),
        title,
        "teacher-reassigned",
        model,
        new DispatchService.InApp(
            "TEACHER_REASSIGNED", title, body + suffix, link, aboutStudentId));
  }

  private static Map<String, Object> renewalModel(
      UserView to, String reason, EnrollmentRenewalDue event, String link) {
    Map<String, Object> model = new HashMap<>();
    model.put("fullName", to.fullName());
    model.put("reasonText", reason);
    model.put("sessionsRemaining", event.sessionsRemaining());
    model.put("formattedExpiresAt", NotificationFormat.date(event.expiresAt(), to.timezone()));
    model.put("link", link);
    return model;
  }

  private static String reasonText(EnrollmentRenewalDue event, String timezone) {
    return event.reason() == RenewalReason.LOW_SESSIONS
        ? "Gói học sắp hết buổi (còn " + event.sessionsRemaining() + " buổi)."
        : "Gói học hết hạn ngày " + NotificationFormat.date(event.expiresAt(), timezone) + ".";
  }
}
