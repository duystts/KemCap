package com.englishplatform.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.CatalogEvents.EnrollmentRenewalDue;
import com.englishplatform.catalog.CatalogEvents.RenewalReason;
import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.catalog.internal.jobs.EnrollmentLifecycleJob;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.scheduling.internal.availability.AvailabilityRule;
import com.englishplatform.scheduling.internal.availability.AvailabilityRuleRepository;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.support.ApiTestSupport;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Job hằng ngày (FR-024, FR-026): hết hạn, nhắc gia hạn mỗi lý do một lần, gia hạn thủ công. */
@RecordApplicationEvents
class EnrollmentLifecycleJobIT extends ApiTestSupport {

  private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final int VALIDITY_DAYS = 90;

  @Autowired EnrollmentLifecycleJob job;
  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired EnrollmentApi enrollmentApi;
  @Autowired AvailabilityRuleRepository rules;
  @Autowired PlatformTransactionManager txManager;
  @Autowired ApplicationEvents events;
  @Autowired JdbcTemplate jdbc;

  private record Setup(TestUser admin, TestUser teacher, TestUser student, UUID enrollmentId) {}

  private Setup setup(int sessions) throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    for (DayOfWeek d : DayOfWeek.values()) {
      rules.save(new AvailabilityRule(teacher.id(), d, LocalTime.of(6, 0), LocalTime.of(22, 0)));
    }
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói", sessions, 45, VALIDITY_DAYS, 1_000_000));
    UUID id =
        enrollmentService.create(student.id(), pkg.getId(), teacher.id(), null, admin.id()).getId();
    inTx(() -> enrollmentApi.activateOnFirstPayment(id));
    return new Setup(admin, teacher, student, id);
  }

  private void inTx(Runnable action) {
    new TransactionTemplate(txManager).executeWithoutResult(s -> action.run());
  }

  private List<RenewalReason> reminders(UUID enrollmentId) {
    return events.stream(EnrollmentRenewalDue.class)
        .filter(e -> e.enrollmentId().equals(enrollmentId))
        .map(EnrollmentRenewalDue::reason)
        .toList();
  }

  private Instant daysFromClock(int days, int hour) {
    return LocalDate.ofInstant(clock.instant(), VN)
        .plusDays(days)
        .atTime(hour, 0)
        .atZone(VN)
        .toInstant();
  }

  private String bookBody(UUID enrollmentId, Instant startsAt) {
    return "{\"enrollmentId\":\"" + enrollmentId + "\",\"startsAt\":\"" + startsAt + "\"}";
  }

  @Test
  void expiredEnrollmentKeepsBookedSessionsAndExtendReactivates() throws Exception {
    Setup s = setup(6);
    UUID sessionId =
        UUID.fromString(
            read(
                call(
                        post("/api/v1/sessions"),
                        s.student(),
                        bookBody(s.enrollmentId(), daysFromClock(3, 10)))
                    .andExpect(status().isCreated()),
                "$.id"));

    job.run();
    assertThat(enrollmentApi.get(s.enrollmentId()).status()).isEqualTo(EnrollmentStatus.ACTIVE);

    clock.advance(Duration.ofDays(VALIDITY_DAYS + 1));
    job.run();

    assertThat(enrollmentApi.get(s.enrollmentId()).status()).isEqualTo(EnrollmentStatus.EXPIRED);
    assertThat(
            jdbc.queryForObject("select status from session where id = ?", String.class, sessionId))
        .as("buổi đã đặt vẫn giữ")
        .isEqualTo("BOOKED");
    call(post("/api/v1/sessions"), s.admin(), bookBody(s.enrollmentId(), daysFromClock(3, 12)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_ACTIVE"));

    Instant newExpiry = clock.instant().plus(Duration.ofDays(30));
    call(
            post("/api/v1/enrollments/" + s.enrollmentId() + "/extend"),
            s.admin(),
            "{\"newExpiresAt\":\"" + newExpiry + "\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    call(post("/api/v1/sessions"), s.admin(), bookBody(s.enrollmentId(), daysFromClock(3, 12)))
        .andExpect(status().isCreated());
  }

  /** F3 (U2): hết hạn chặn đặt lịch ngay cả khi job chưa chạy. */
  @Test
  void reserveIsBlockedAfterExpiryBeforeJobRuns() throws Exception {
    Setup s = setup(6);
    clock.advance(Duration.ofDays(VALIDITY_DAYS + 1));

    assertThat(enrollmentApi.get(s.enrollmentId()).status()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThatThrownBy(() -> inTx(() -> enrollmentApi.reserve(s.enrollmentId())))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> assertThat(e.code().name()).isEqualTo("ENROLLMENT_NOT_ACTIVE"));
  }

  /** F2: gói quá hạn mà job chưa chạy: đặt buổi báo đúng mã lỗi để FE hiện đúng thông báo. */
  @Test
  void bookingAfterExpiryBeforeJobRunsReportsEnrollmentNotActive() throws Exception {
    Setup s = setup(6);
    clock.advance(Duration.ofDays(VALIDITY_DAYS + 1));

    call(post("/api/v1/sessions"), s.admin(), bookBody(s.enrollmentId(), daysFromClock(3, 12)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_ACTIVE"));
  }

  /** R22: trẻ chưa có đồng ý của người giám hộ chưa nhận bất kỳ email nào (FR-030). */
  @Test
  void studentAwaitingGuardianConsentIsNotReminded() throws Exception {
    Setup s = setup(2);
    UserAccount child =
        users.save(
            new UserAccount(
                uniqueEmail(),
                "Bé Na",
                Role.STUDENT,
                LocalDate.now(VN).minusYears(12),
                UserStatus.AWAITING_GUARDIAN_CONSENT));
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói", 2, 45, VALIDITY_DAYS, 1_000_000));
    UUID id =
        enrollmentService
            .create(child.getId(), pkg.getId(), s.teacher().id(), null, s.admin().id())
            .getId();
    inTx(() -> enrollmentApi.activateOnFirstPayment(id));

    job.run();

    assertThat(reminders(id)).isEmpty();
  }

  /** R2: học viên đang ngừng không bị nhắc và không mất lượt nhắc khi được kích hoạt lại. */
  @Test
  void inactiveStudentIsRemindedOnlyAfterReactivation() throws Exception {
    Setup s = setup(2);
    call(
            patch("/api/v1/users/" + s.student().id() + "/status"),
            s.admin(),
            "{\"status\":\"INACTIVE\"}")
        .andExpect(status().isOk());

    job.run();
    assertThat(reminders(s.enrollmentId())).isEmpty();

    call(
            patch("/api/v1/users/" + s.student().id() + "/status"),
            s.admin(),
            "{\"status\":\"ACTIVE\"}")
        .andExpect(status().isOk());
    job.run();
    assertThat(reminders(s.enrollmentId())).containsExactly(RenewalReason.LOW_SESSIONS);
  }

  @Test
  void lowSessionsReminderFiresOnce() throws Exception {
    Setup many = setup(6);
    Setup low = setup(2);

    job.run();
    job.run();

    assertThat(reminders(low.enrollmentId())).containsExactly(RenewalReason.LOW_SESSIONS);
    assertThat(reminders(many.enrollmentId())).isEmpty();
  }

  @Test
  void expiringReminderFiresOnceEvenAfterExtend() throws Exception {
    Setup s = setup(6);

    clock.advance(Duration.ofDays(VALIDITY_DAYS - 8));
    job.run();
    assertThat(reminders(s.enrollmentId())).isEmpty();

    clock.advance(Duration.ofDays(2));
    job.run();
    job.run();
    assertThat(reminders(s.enrollmentId())).containsExactly(RenewalReason.EXPIRING);

    call(
            post("/api/v1/enrollments/" + s.enrollmentId() + "/extend"),
            s.admin(),
            "{\"newExpiresAt\":\"" + clock.instant().plus(Duration.ofDays(3)) + "\"}")
        .andExpect(status().isOk());
    job.run();
    assertThat(reminders(s.enrollmentId())).containsExactly(RenewalReason.EXPIRING);
  }

  @Test
  void eachReasonRemindsSeparately() throws Exception {
    Setup s = setup(2);
    job.run();

    clock.advance(Duration.ofDays(VALIDITY_DAYS - 3));
    job.run();

    assertThat(reminders(s.enrollmentId()))
        .containsExactly(RenewalReason.LOW_SESSIONS, RenewalReason.EXPIRING);
  }

  /**
   * Gói dùng hết buổi trong một ngày vẫn được nhắc (FR-026: còn ≤ 2 buổi), không đổi trạng thái.
   */
  @Test
  void exhaustedEnrollmentStillGetsLowSessionsReminder() throws Exception {
    Setup s = setup(1);
    inTx(
        () -> {
          enrollmentApi.reserve(s.enrollmentId());
          enrollmentApi.consume(s.enrollmentId());
        });
    assertThat(enrollmentApi.get(s.enrollmentId()).status()).isEqualTo(EnrollmentStatus.EXHAUSTED);

    job.run();

    assertThat(reminders(s.enrollmentId())).containsExactly(RenewalReason.LOW_SESSIONS);
    assertThat(enrollmentApi.get(s.enrollmentId()).status()).isEqualTo(EnrollmentStatus.EXHAUSTED);
  }

  /** R8 (U7): học viên đã ẩn danh không còn ai để nhắc, nhưng ghi danh vẫn hết hạn bình thường. */
  @Test
  void anonymizedStudentIsNotRemindedButStillExpires() throws Exception {
    Setup s = setup(2);
    String request =
        read(
            call(
                    post("/api/v1/data-subject-requests"),
                    s.admin(),
                    "{\"studentId\":\""
                        + s.student().id()
                        + "\",\"requesterName\":\"A\",\"type\":\"ERASURE\",\"receivedOn\":\""
                        + LocalDate.now(VN)
                        + "\"}")
                .andExpect(status().isCreated()),
            "$.id");
    call(
            post("/api/v1/data-subject-requests/" + request + "/anonymize"),
            s.admin(),
            "{\"confirm\":true}")
        .andExpect(status().isOk());

    job.run();
    assertThat(reminders(s.enrollmentId())).isEmpty();

    clock.advance(Duration.ofDays(VALIDITY_DAYS + 1));
    job.run();
    assertThat(reminders(s.enrollmentId())).isEmpty();
    assertThat(enrollmentApi.get(s.enrollmentId()).status()).isEqualTo(EnrollmentStatus.EXPIRED);
  }
}
