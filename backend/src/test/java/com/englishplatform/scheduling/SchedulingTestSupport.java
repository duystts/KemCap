package com.englishplatform.scheduling;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.Role;
import com.englishplatform.scheduling.internal.availability.AvailabilityRule;
import com.englishplatform.scheduling.internal.availability.AvailabilityRuleRepository;
import com.englishplatform.scheduling.internal.session.SessionRepository;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.support.ApiTestSupport;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Ghi danh ACTIVE + giáo viên rảnh 06:00–22:00 mọi ngày (giờ VN) để test đặt lịch. */
abstract class SchedulingTestSupport extends ApiTestSupport {

  static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired EnrollmentApi enrollmentApi;
  @Autowired AvailabilityRuleRepository rules;
  @Autowired SessionRepository sessions;
  @Autowired PlatformTransactionManager txManager;

  /** Một cặp giáo viên–học viên với ghi danh ACTIVE. */
  record Setup(TestUser admin, TestUser teacher, TestUser student, UUID enrollmentId) {
    Setup withEnrollment(SchedulingTestSupport t, int sessionCount) {
      return new Setup(
          admin, teacher, student, t.enroll(student.id(), teacher.id(), admin.id(), sessionCount));
    }
  }

  Setup setup(int sessionCount) throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    availableAllWeek(teacher.id());
    return new Setup(admin, teacher, user(Role.STUDENT), null).withEnrollment(this, sessionCount);
  }

  /** Thêm học viên mới học cùng giáo viên. */
  Setup anotherStudent(Setup base, int sessionCount) throws Exception {
    return new Setup(base.admin(), base.teacher(), user(Role.STUDENT), null)
        .withEnrollment(this, sessionCount);
  }

  UUID enroll(UUID studentId, UUID teacherId, UUID adminId, int sessionCount) {
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói", sessionCount, 45, 90, 1_000_000));
    UUID id = enrollmentService.create(studentId, pkg.getId(), teacherId, null, adminId).getId();
    inTx(() -> enrollmentApi.activateOnFirstPayment(id));
    return id;
  }

  void availableAllWeek(UUID teacherId) {
    for (DayOfWeek d : DayOfWeek.values()) {
      rules.save(new AvailabilityRule(teacherId, d, LocalTime.of(6, 0), LocalTime.of(22, 0)));
    }
  }

  /** {@code days} ngày nữa lúc {@code hour}:{@code minute} giờ VN. */
  static Instant at(int days, int hour, int minute) {
    return LocalDate.now(VN).plusDays(days).atTime(hour, minute).atZone(VN).toInstant();
  }

  ResultActions book(TestUser as, UUID enrollmentId, Instant startsAt) throws Exception {
    return call(
        post("/api/v1/sessions"),
        as,
        "{\"enrollmentId\":\"" + enrollmentId + "\",\"startsAt\":\"" + startsAt + "\"}");
  }

  UUID bookOk(TestUser as, UUID enrollmentId, Instant startsAt) throws Exception {
    return UUID.fromString(read(book(as, enrollmentId, startsAt), "$.id"));
  }

  EnrollmentView enrollment(UUID id) {
    return enrollmentApi.get(id);
  }

  static CurrentUser actor(TestUser u) {
    return new CurrentUser(u.id(), u.role().name(), "Người dùng " + u.role(), "Asia/Ho_Chi_Minh");
  }

  /** Bọc checked exception cho lambda của Modulith {@code Scenario}. */
  static <T> T unchecked(java.util.concurrent.Callable<T> action) {
    try {
      return action.call();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  void inTx(Runnable action) {
    new TransactionTemplate(txManager).executeWithoutResult(s -> action.run());
  }
}
