package com.englishplatform.notification.internal;

import com.englishplatform.catalog.CatalogEvents;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Xử lý thông báo cho các sự kiện ghi danh & gói học (T133). */
@Component
class CatalogNotificationListener {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

  @ApplicationModuleListener
  void on(CatalogEvents.TeacherReassigned event) {
    UserView student = userDirectory.find(event.studentId()).orElse(null);
    UserView fromTeacher = userDirectory.find(event.fromTeacherId()).orElse(null);
    UserView toTeacher = userDirectory.find(event.toTeacherId()).orElse(null);

    if (student == null || fromTeacher == null || toTeacher == null) return;

    // 1. Thông báo học viên
    String studentLink = frontendUrl + "/student/packages";
    Map<String, Object> studentModel = new HashMap<>();
    studentModel.put("fullName", student.fullName());
    studentModel.put(
        "message",
        "Giáo viên phụ trách của bạn đã được thay đổi từ thầy/cô "
            + fromTeacher.fullName()
            + " sang thầy/cô "
            + toTeacher.fullName()
            + ".");
    studentModel.put("link", studentLink);

    dispatchService.notify(
        "enrollment:" + event.enrollmentId() + ":reassigned:" + toTeacher.id() + ":student",
        student.id(),
        student.email(),
        "Thông báo đổi giáo viên phụ trách",
        "teacher-reassigned",
        studentModel,
        new DispatchService.InApp(
            "TEACHER_REASSIGNED",
            "Đổi giáo viên phụ trách",
            "Giáo viên phụ trách mới của bạn là " + toTeacher.fullName(),
            studentLink));

    // 2. Thông báo giáo viên cũ
    String fromLink = frontendUrl + "/teacher/students";
    Map<String, Object> fromModel = new HashMap<>();
    fromModel.put("fullName", fromTeacher.fullName());
    fromModel.put(
        "message",
        "Học viên "
            + student.fullName()
            + " đã được chuyển giao cho giáo viên "
            + toTeacher.fullName()
            + " phụ trách.");
    fromModel.put("link", fromLink);

    dispatchService.notify(
        "enrollment:" + event.enrollmentId() + ":reassigned:" + toTeacher.id() + ":from-teacher",
        fromTeacher.id(),
        fromTeacher.email(),
        "Thông báo bàn giao học viên " + student.fullName(),
        "teacher-reassigned",
        fromModel,
        new DispatchService.InApp(
            "TEACHER_REASSIGNED",
            "Bàn giao học viên",
            "Học viên " + student.fullName() + " đã chuyển sang GV " + toTeacher.fullName(),
            fromLink));

    // 3. Thông báo giáo viên mới
    String toLink = frontendUrl + "/teacher/students";
    Map<String, Object> toModel = new HashMap<>();
    toModel.put("fullName", toTeacher.fullName());
    toModel.put(
        "message",
        "Bạn đã được phân công phụ trách học viên mới: "
            + student.fullName()
            + ". Hãy xem hồ sơ và liên hệ học viên nhé!");
    toModel.put("link", toLink);

    dispatchService.notify(
        "enrollment:" + event.enrollmentId() + ":reassigned:" + toTeacher.id() + ":to-teacher",
        toTeacher.id(),
        toTeacher.email(),
        "Phân công học viên mới: " + student.fullName(),
        "teacher-reassigned",
        toModel,
        new DispatchService.InApp(
            "TEACHER_REASSIGNED",
            "Học viên mới được phân công",
            "Bạn được phân công phụ trách học viên " + student.fullName(),
            toLink));
  }

  @ApplicationModuleListener
  void on(CatalogEvents.EnrollmentActivated event) {
    userDirectory
        .find(event.studentId())
        .ifPresent(
            student -> {
              String expiresAtFormatted = formatDate(event.expiresAt(), student.timezone());
              String link = frontendUrl + "/student/packages";

              Map<String, Object> model = new HashMap<>();
              model.put("fullName", student.fullName());
              model.put("formattedExpiresAt", expiresAtFormatted);
              model.put("link", link);

              dispatchService.notify(
                  "enrollment:" + event.enrollmentId() + ":activated",
                  student.id(),
                  student.email(),
                  "Gói học của bạn đã được kích hoạt thành công",
                  "enrollment-activated",
                  model,
                  new DispatchService.InApp(
                      "ENROLLMENT_ACTIVATED",
                      "Gói học đã kích hoạt",
                      "Gói học tiếng Anh đã sẵn sàng. Hạn dùng: " + expiresAtFormatted,
                      link));
            });
  }

  @ApplicationModuleListener
  void on(CatalogEvents.EnrollmentRenewalDue event) {
    userDirectory
        .find(event.studentId())
        .ifPresent(
            student -> {
              String expiresAtFormatted = formatDate(event.expiresAt(), student.timezone());
              String reasonText =
                  (event.reason() == CatalogEvents.RenewalReason.LOW_SESSIONS)
                      ? "Gói học của bạn sắp hết buổi (chỉ còn " + event.sessionsRemaining() + " buổi)."
                      : "Gói học của bạn sắp hết hạn sử dụng vào ngày " + expiresAtFormatted + ".";

              String studentLink = frontendUrl + "/student/packages";

              // Báo học viên
              Map<String, Object> model = new HashMap<>();
              model.put("fullName", student.fullName());
              model.put("reasonText", reasonText);
              model.put("sessionsRemaining", event.sessionsRemaining());
              model.put("formattedExpiresAt", expiresAtFormatted);
              model.put("link", studentLink);

              dispatchService.notify(
                  "enrollment:"
                      + event.enrollmentId()
                      + ":renewal:"
                      + event.reason().name()
                      + ":student",
                  student.id(),
                  student.email(),
                  "Nhắc gia hạn gói học English 1-1",
                  "renewal-reminder",
                  model,
                  new DispatchService.InApp(
                      "RENEWAL_DUE",
                      "Nhắc gia hạn gói học",
                      reasonText,
                      studentLink));

              // Báo admin
              for (UserView admin : userDirectory.findAdmins()) {
                Map<String, Object> adminModel = new HashMap<>();
                adminModel.put("fullName", admin.fullName());
                adminModel.put(
                    "reasonText",
                    "Học viên " + student.fullName() + " có gói học cần gia hạn (" + reasonText + ").");
                adminModel.put("sessionsRemaining", event.sessionsRemaining());
                adminModel.put("formattedExpiresAt", expiresAtFormatted);
                adminModel.put("link", frontendUrl + "/admin/billing");

                dispatchService.notify(
                    "enrollment:"
                        + event.enrollmentId()
                        + ":renewal:"
                        + event.reason().name()
                        + ":admin:"
                        + admin.id(),
                    admin.id(),
                    admin.email(),
                    "[Gia hạn] Học viên " + student.fullName() + " sắp hết gói học",
                    "renewal-reminder",
                    adminModel,
                    new DispatchService.InApp(
                        "RENEWAL_DUE",
                        "Học viên cần gia hạn",
                        "HV " + student.fullName() + " - " + reasonText,
                        frontendUrl + "/admin/billing"));
              }
            });
  }

  private static String formatDate(Instant instant, String timezone) {
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
    return instant.atZone(zoneId).format(DATE_FORMATTER);
  }
}
