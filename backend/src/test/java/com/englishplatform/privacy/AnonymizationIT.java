package com.englishplatform.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.PasswordResetRequested;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.identity.UserInvited;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.notification.EmailSender.EmailMessage;
import com.englishplatform.notification.internal.FakeEmailSender;
import com.englishplatform.scheduling.SchedulingEvents.ReminderWindow;
import com.englishplatform.scheduling.SchedulingEvents.SessionReminderDue;
import com.englishplatform.scheduling.internal.availability.AvailabilityRule;
import com.englishplatform.scheduling.internal.availability.AvailabilityRuleRepository;
import com.englishplatform.support.ApiTestSupport;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ẩn danh hóa xuyên module (FR-031, SC-007): định danh mất, số liệu không đổi. Báo cáo {@code
 * /reports/revenue}, {@code /receivables}, {@code /reports/teacher-sessions} thuộc U8 nên ở đây so
 * bằng dữ liệu gốc của chúng; U8 chạy lại test này với báo cáo thật. U6 mở rộng phần phiếu, bài
 * tập, đánh giá sao.
 */
class AnonymizationIT extends ApiTestSupport {

  private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired EnrollmentApi enrollmentApi;
  @Autowired AvailabilityRuleRepository rules;
  @Autowired FakeEmailSender emailSender;
  @Autowired JdbcTemplate jdbc;
  @Autowired ApplicationEventPublisher publisher;
  @Autowired PlatformTransactionManager txManager;

  @BeforeEach
  @AfterEach
  void clearOutbox() {
    emailSender.clear();
  }

  /** Số liệu SC-007 có sẵn trước U8: đã thu, công nợ, số buổi đã dạy, bộ đếm ghi danh đã học. */
  private record Figures(
      String paid,
      String debt,
      long taught,
      int consumed,
      String revenueCollected,
      String revenueDebt,
      String receivableDebt,
      String reportTaught) {}

  private Figures figures(TestUser admin, TestUser teacher, UUID enrollmentId) throws Exception {
    var statement = call(get("/api/v1/enrollments/" + enrollmentId + "/payments"), admin);
    var revenue = call(get("/api/v1/reports/revenue"), admin).andExpect(status().isOk());
    return new Figures(
        read(statement, "$.amountPaidVnd"),
        read(statement, "$.debtVnd"),
        jdbc.queryForObject(
            "select count(*) from session where teacher_id = ? and status = 'COMPLETED'",
            Long.class,
            teacher.id()),
        enrollmentApi.get(enrollmentId).sessionsConsumed(),
        read(revenue, "$.collectedVnd"),
        read(revenue, "$.debtVnd"),
        read(
            call(get("/api/v1/receivables?hasDebt=true"), admin),
            "$[?(@.enrollmentId == '" + enrollmentId + "')].debtVnd"),
        read(
            call(get("/api/v1/reports/teacher-sessions"), admin),
            "$[?(@.teacherId == '" + teacher.id() + "')].sessionsTaught"));
  }

  private void availableAllWeek(TestUser teacher) {
    for (DayOfWeek d : DayOfWeek.values()) {
      rules.save(new AvailabilityRule(teacher.id(), d, LocalTime.of(6, 0), LocalTime.of(22, 0)));
    }
  }

  private UUID enrollWithPartialPayment(TestUser admin, TestUser teacher, TestUser student)
      throws Exception {
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói", 6, 45, 90, 1_000_000));
    UUID id =
        enrollmentService.create(student.id(), pkg.getId(), teacher.id(), null, admin.id()).getId();
    call(
            post("/api/v1/enrollments/" + id + "/payments"),
            admin,
            "{\"amountVnd\":400000,\"paidAt\":\"" + LocalDate.now(VN) + "\",\"method\":\"CASH\"}")
        .andExpect(status().isCreated());
    return id;
  }

  private static Instant at(int days, int hour) {
    return LocalDate.now(VN).plusDays(days).atTime(hour, 0).atZone(VN).toInstant();
  }

  private UUID book(TestUser student, UUID enrollmentId, Instant startsAt) throws Exception {
    return UUID.fromString(
        read(
            call(
                    post("/api/v1/sessions"),
                    student,
                    "{\"enrollmentId\":\"" + enrollmentId + "\",\"startsAt\":\"" + startsAt + "\"}")
                .andExpect(status().isCreated()),
            "$.id"));
  }

  private UUID request(TestUser admin, UUID studentId, String type) throws Exception {
    return UUID.fromString(
        read(
            call(
                    post("/api/v1/data-subject-requests"),
                    admin,
                    "{\"studentId\":\""
                        + studentId
                        + "\",\"requesterName\":\"Trần Thị Mẹ\",\"type\":\""
                        + type
                        + "\",\"receivedOn\":\""
                        + LocalDate.now(VN)
                        + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED")),
            "$.id"));
  }

  private org.springframework.test.web.servlet.ResultActions anonymize(TestUser admin, UUID request)
      throws Exception {
    return call(
        post("/api/v1/data-subject-requests/" + request + "/anonymize"),
        admin,
        "{\"confirm\":true}");
  }

  /** Mọi listener của các event nhắc tới học viên đã chạy xong không lỗi. */
  private void awaitListenersDone(UUID studentId) {
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "select count(*) from event_publication where serialized_event like ?",
                            Long.class,
                            "%" + studentId + "%"))
                    .isZero());
  }

  @Test
  void anonymizationRemovesIdentityAndKeepsFigures() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    // tên và số điện thoại riêng để quét toàn CSDL; các ô ghi chú tự do cũng nhắc tên (F4)
    String name = "Hoc Vien " + UUID.randomUUID().toString().substring(0, 8);
    String phone = "09" + (10_000_000 + new java.util.Random().nextInt(89_999_999));
    call(
            patch("/api/v1/me"),
            student,
            "{\"fullName\":\"" + name + "\",\"phone\":\"" + phone + "\"}")
        .andExpect(status().isOk());
    TestUser previousTeacher = user(Role.TEACHER);
    availableAllWeek(previousTeacher);
    UUID enrollmentId = enrollWithPartialPayment(admin, previousTeacher, student);
    availableAllWeek(teacher);
    call(
            put("/api/v1/enrollments/" + enrollmentId + "/teacher"),
            admin,
            "{\"teacherId\":\"" + teacher.id() + "\",\"reason\":\"Yêu cầu của " + name + "\"}")
        .andExpect(status().isOk());
    awaitListenersDone(student.id());
    Instant pastStart = at(2, 10);
    UUID done = book(student, enrollmentId, pastStart);
    UUID cancelledEnrollment = enrollWithPartialPayment(admin, teacher, student);
    call(
            post("/api/v1/enrollments/" + cancelledEnrollment + "/cancel"),
            admin,
            "{\"reason\":\"" + name + " chuyển nơi học\"}")
        .andExpect(status().isOk());
    UUID cancelled = book(student, enrollmentId, at(4, 10));
    UUID future = book(student, enrollmentId, at(6, 10));
    call(
            post("/api/v1/sessions/" + cancelled + "/cancel"),
            student,
            "{\"reason\":\"" + name + " bận việc\"}")
        .andExpect(status().isOk());
    call(
            put("/api/v1/students/" + student.id() + "/profile"),
            admin,
            "{\"selfReportedLevel\":\"A2\",\"occupation\":\"Sinh viên\",\"notes\":\"Hay đi muộn\"}")
        .andExpect(status().isOk());
    call(
            post("/api/v1/students/" + student.id() + "/goals"),
            teacher,
            "{\"type\":\"IELTS\",\"target\":\"6.5\"}")
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + student.email() + "\"}"))
        .andExpect(status().isAccepted());

    clock.set(pastStart.plus(Duration.ofMinutes(50)));
    call(
            post("/api/v1/sessions/" + done + "/outcome"),
            teacher,
            "{\"outcome\":\"COMPLETED\",\"note\":\"" + name + " học tốt\"}")
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    Figures before = figures(admin, teacher, enrollmentId);
    assertThat(before.paid()).isEqualTo("400000");
    assertThat(before.debt()).isEqualTo("600000");
    assertThat(before.taught()).isEqualTo(1);
    assertThat(before.consumed()).isEqualTo(1);
    assertThat(before.receivableDebt()).isEqualTo("[600000]");
    assertThat(before.reportTaught()).isEqualTo("[1]");
    assertThat(enrollmentApi.get(enrollmentId).sessionsReserved()).isEqualTo(1);

    // chờ listener để không có dữ liệu ghi muộn; quét phải thấy dữ liệu thì "không còn" mới có
    // nghĩa
    awaitListenersDone(student.id());
    assertThat(columnsContaining(student.email())).contains("user_account.email");
    assertThat(columnsContaining(name)).contains("user_account.full_name", "session.cancel_reason");
    assertThat(columnsContaining(phone)).contains("user_account.phone");

    UUID requestId = request(admin, student.id(), "ERASURE");
    anonymize(admin, requestId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.handledBy").value(admin.id().toString()))
        .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

    // định danh mất ngay trong cùng transaction
    call(get("/api/v1/users/" + student.id()), admin)
        .andExpect(jsonPath("$.email").value("anonymized+" + student.id() + "@invalid"))
        .andExpect(jsonPath("$.fullName").value("Học viên đã ẩn danh"))
        .andExpect(jsonPath("$.phone").isEmpty())
        .andExpect(jsonPath("$.dateOfBirth").isEmpty())
        .andExpect(jsonPath("$.status").value("INACTIVE"))
        .andExpect(jsonPath("$.anonymized").value(true));
    assertThat(users.findById(student.id()).orElseThrow().getPasswordHash()).isNull();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auth_token where user_id = ? and used_at is null",
                Long.class,
                student.id()))
        .as("mọi token REFRESH, INVITE, PASSWORD_RESET đều bị thu hồi")
        .isZero();

    awaitListenersDone(student.id());

    // listener các module đã dọn xong
    assertThat(jdbc.queryForObject("select status from session where id = ?", String.class, future))
        .isEqualTo("CANCELLED_SYSTEM");
    assertThat(jdbc.queryForObject("select status from session where id = ?", String.class, done))
        .isEqualTo("COMPLETED");
    EnrollmentView enrollment = enrollmentApi.get(enrollmentId);
    assertThat(enrollment.sessionsReserved()).as("buổi tương lai được hoàn").isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from learning_goal where student_id = ?",
                Long.class,
                student.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from student_profile where user_id = ? and"
                    + " (notes is not null or occupation is not null"
                    + " or self_reported_level is not null)",
                Long.class,
                student.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from notification where user_id = ? or about_student_id = ?",
                Long.class,
                student.id(),
                student.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from notification_dispatch"
                    + " where user_id = ? and recipient <> 'anonymized'",
                Long.class,
                student.id()))
        .isZero();

    // SC-007: số liệu không đổi
    assertThat(figures(admin, teacher, enrollmentId)).isEqualTo(before);

    // định danh không còn ở bất kỳ cột văn bản nào (R10); U6 kế thừa khi thêm bảng mới
    assertThat(columnsContaining(student.email())).isEmpty();
    assertThat(columnsContaining(name)).isEmpty();
    assertThat(columnsContaining(phone)).isEmpty();

    // yêu cầu giữ tên người yêu cầu làm bằng chứng tuân thủ
    call(get("/api/v1/data-subject-requests?studentId=" + student.id()), admin)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].requesterName").value("Trần Thị Mẹ"))
        .andExpect(jsonPath("$[0].status").value("COMPLETED"));

    // gọi lại API → 409; phát lại event → không lỗi, không đổi số liệu
    anonymize(admin, requestId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
    new TransactionTemplate(txManager)
        .executeWithoutResult(
            s -> publisher.publishEvent(new StudentAnonymized(student.id(), requestId)));
    awaitListenersDone(student.id());
    assertThat(figures(admin, teacher, enrollmentId)).isEqualTo(before);
  }

  /**
   * Cột văn bản còn chứa {@code value}. Miễn trừ có chủ đích: {@code requester_name} là bằng chứng
   * tuân thủ, {@code payment.note} là chứng từ tài chính (research.md, rủi ro còn lại).
   */
  private List<String> columnsContaining(String value) {
    List<String> found = new ArrayList<>();
    jdbc.queryForList(
            "select table_name, column_name from information_schema.columns"
                + " where table_schema = 'public'"
                + " and data_type in ('text', 'character varying')"
                + " and table_name <> 'flyway_schema_history'")
        .forEach(
            c -> {
              String column = c.get("table_name") + "." + c.get("column_name");
              if (!Set.of("data_subject_request.requester_name", "payment.note").contains(column)
                  && jdbc.queryForObject(
                          "select count(*) from \"%s\" where \"%s\" like ?"
                              .formatted(c.get("table_name"), c.get("column_name")),
                          Long.class,
                          "%" + value + "%")
                      > 0) {
                found.add(column);
              }
            });
    return found;
  }

  /** R1: ghi của một request đã đọc hồ sơ từ trước không được khôi phục định danh. */
  @Test
  void staleWriteCannotRestoreIdentity() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    UserAccount stale = users.findById(student.id()).orElseThrow();

    anonymize(admin, request(admin, student.id(), "ERASURE")).andExpect(status().isOk());

    stale.updateProfile("Tên cũ", "0900000000", null);
    assertThatThrownBy(() -> users.save(stale))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    UserAccount current = users.findById(student.id()).orElseThrow();
    assertThat(current.isAnonymized()).isTrue();
    assertThat(current.getFullName()).isEqualTo("Học viên đã ẩn danh");
    assertThat(current.getEmail()).endsWith("@invalid");
  }

  /** R1: đăng nhập cùng lúc không va version, và không ghi đè hồ sơ đã ẩn danh. */
  @Test
  void loginDoesNotBumpAccountVersion() throws Exception {
    TestUser student = user(Role.STUDENT);
    Long before =
        jdbc.queryForObject(
            "select version from user_account where id = ?", Long.class, student.id());

    login(student.email());

    assertThat(
            jdbc.queryForObject(
                "select version from user_account where id = ?", Long.class, student.id()))
        .isEqualTo(before);
    assertThat(users.findById(student.id()).orElseThrow().getLastLoginAt()).isNotNull();
  }

  /** R2: event mời/đặt lại mật khẩu còn trong hàng đợi tới sau lúc ẩn danh hóa. */
  @Test
  void lateIdentityEventsDoNotEmailOldAddress() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    anonymize(admin, request(admin, student.id(), "ERASURE")).andExpect(status().isOk());
    awaitListenersDone(student.id());

    new TransactionTemplate(txManager)
        .executeWithoutResult(
            s -> {
              publisher.publishEvent(
                  new UserInvited(
                      student.id(),
                      student.email(),
                      "Tên cũ",
                      Role.STUDENT,
                      UUID.randomUUID(),
                      "raw-token"));
              publisher.publishEvent(
                  new PasswordResetRequested(
                      student.id(), student.email(), UUID.randomUUID(), "raw-token"));
            });
    awaitListenersDone(student.id());

    assertThat(emailSender.sent()).extracting(EmailMessage::to).doesNotContain(student.email());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from notification_dispatch where recipient = ?",
                Long.class,
                student.email()))
        .isZero();
  }

  /** N1: buổi đã qua vẫn chốt được (SC-007), nhưng văn bản tự do mới không được lưu. */
  @Test
  void closingPastSessionAfterAnonymizationStoresNoFreeText() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    availableAllWeek(teacher);
    UUID enrollmentId = enrollWithPartialPayment(admin, teacher, student);
    Instant start = at(1, 10);
    UUID completed = book(student, enrollmentId, start);
    UUID absent = book(student, enrollmentId, at(1, 14));
    clock.set(at(1, 15));
    anonymize(admin, request(admin, student.id(), "ERASURE")).andExpect(status().isOk());
    awaitListenersDone(student.id());

    call(
            post("/api/v1/sessions/" + completed + "/outcome"),
            teacher,
            "{\"outcome\":\"COMPLETED\",\"note\":\"Nhắc tên học viên\"}")
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    call(
            post("/api/v1/sessions/" + absent + "/cancel"),
            teacher,
            "{\"reason\":\"Nhắc tên học viên\"}")
        .andExpect(status().isOk());

    assertThat(columnsContaining("Nhắc tên học viên")).isEmpty();
    assertThat(enrollmentApi.get(enrollmentId).sessionsConsumed()).isEqualTo(1);
  }

  /** N3: đăng nhập chen ngay sau ẩn danh hóa không tạo phiên mới. */
  @Test
  void recordLoginDoesNothingOnAnonymizedAccount() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    anonymize(admin, request(admin, student.id(), "ERASURE")).andExpect(status().isOk());

    Integer updated =
        new TransactionTemplate(txManager)
            .execute(s -> users.recordLogin(student.id(), Instant.now()));
    assertThat(updated).isZero();
  }

  /** R3: đường dẫn mã hóa phần trăm không được vượt cổng tài khoản và đồng ý chính sách. */
  @Test
  void percentEncodedPathDoesNotBypassGate() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser noConsent = user(Role.TEACHER);
    jdbc.update("delete from policy_consent where user_id = ?", noConsent.id());
    call(get(URI.create("/api/v%31/sessions")), noConsent)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));

    TestUser student = user(Role.STUDENT);
    anonymize(admin, request(admin, student.id(), "ERASURE")).andExpect(status().isOk());
    call(get(URI.create("/api/v%31/me")), student).andExpect(status().isUnauthorized());
    call(patch(URI.create("/api/v%31/me")), student, "{\"fullName\":\"Tên cũ\"}")
        .andExpect(status().isUnauthorized());
    assertThat(users.findById(student.id()).orElseThrow().getFullName())
        .isEqualTo("Học viên đã ẩn danh");
  }

  /**
   * R8, N2: học viên đã ẩn danh không còn giữ giáo viên, không còn trong danh sách của giáo viên.
   */
  @Test
  void anonymizedStudentNoLongerAffectsTeacherWork() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    enrollWithPartialPayment(admin, teacher, student);
    call(patch("/api/v1/users/" + teacher.id() + "/status"), admin, "{\"status\":\"INACTIVE\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TEACHER_HAS_STUDENTS"));
    call(get("/api/v1/students"), teacher).andExpect(jsonPath("$.length()").value(1));

    anonymize(admin, request(admin, student.id(), "ERASURE")).andExpect(status().isOk());
    awaitListenersDone(student.id());

    call(get("/api/v1/students"), teacher).andExpect(jsonPath("$.length()").value(0));
    // giáo vụ vẫn thấy hồ sơ để đối soát, nhưng không gắn cờ "Cần tư vấn" dù mục tiêu đã bị xóa
    call(get("/api/v1/students?needsConsulting=true"), admin)
        .andExpect(jsonPath("$[?(@.id == '" + student.id() + "')]").isEmpty());
    call(patch("/api/v1/users/" + teacher.id() + "/status"), admin, "{\"status\":\"INACTIVE\"}")
        .andExpect(status().isOk());
  }

  /** R4: không ghi được dữ liệu cá nhân mới vào hồ sơ đã ẩn danh. */
  @Test
  void anonymizedStudentCannotReceiveNewData() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    availableAllWeek(teacher);
    UUID enrollmentId = enrollWithPartialPayment(admin, teacher, student);
    anonymize(admin, request(admin, student.id(), "ERASURE")).andExpect(status().isOk());
    String base = "/api/v1/students/" + student.id();

    call(put(base + "/profile"), admin, "{\"notes\":\"Nhập lại\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
    call(post(base + "/goals"), teacher, "{\"type\":\"IELTS\",\"target\":\"6.5\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
    call(
            post(base + "/assessments"),
            teacher,
            "{\"type\":\"PLACEMENT\",\"listening\":\"B1\",\"speaking\":\"A2\",\"reading\":\"B1\","
                + "\"writing\":\"A2\",\"grammarVocab\":\"B1\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
    call(
            post("/api/v1/sessions"),
            admin,
            "{\"enrollmentId\":\"" + enrollmentId + "\",\"startsAt\":\"" + at(3, 10) + "\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
    // đọc vẫn được để đối soát số liệu
    call(get(base + "/goals"), admin).andExpect(status().isOk());

    // F1 (U8): không gia hạn gói của hồ sơ đã ẩn danh
    call(
            post("/api/v1/enrollments/" + enrollmentId + "/extend"),
            admin,
            "{\"newExpiresAt\":\"" + clock.instant().plus(Duration.ofDays(30)) + "\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
    // R1 (U8): vẫn hủy được ghi danh nhưng không lưu văn bản tự do mới
    call(
            post("/api/v1/enrollments/" + enrollmentId + "/cancel"),
            admin,
            "{\"reason\":\"Lý do nhắc tên học viên\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.cancelReason").isEmpty())
        .andExpect(jsonPath("$.cancelledBy").value(admin.id().toString()));
    assertThat(columnsContaining("Lý do nhắc tên học viên")).isEmpty();
  }

  @Test
  void anonymizedStudentCanNeitherLogInNorBeReactivated() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    anonymize(admin, request(admin, student.id(), "ERASURE")).andExpect(status().isOk());

    // access token còn hạn cũng bị chặn ngay
    call(get("/api/v1/me"), student).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"" + student.email() + "\",\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isUnauthorized());
    call(patch("/api/v1/users/" + student.id() + "/status"), admin, "{\"status\":\"ACTIVE\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
    // yêu cầu mới, đồng ý giám hộ trên hồ sơ đã ẩn danh → 409
    call(
            post("/api/v1/data-subject-requests"),
            admin,
            "{\"studentId\":\""
                + student.id()
                + "\",\"requesterName\":\"A\",\"type\":\"ACCESS\",\"receivedOn\":\""
                + LocalDate.now(VN)
                + "\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
    call(
            put("/api/v1/students/" + student.id() + "/guardian-consent"),
            admin,
            "{\"guardianName\":\"A\",\"relationship\":\"Mẹ\",\"guardianContact\":\"09\","
                + "\"consentedOn\":\"2026-01-15\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
    assertThat(users.findById(student.id()).orElseThrow().getStatus())
        .isEqualTo(UserStatus.INACTIVE);
  }

  @Test
  void guardianConsentIsDeleted() throws Exception {
    TestUser admin = user(Role.ADMIN);
    String child =
        read(
            call(
                    post("/api/v1/users"),
                    admin,
                    "{\"email\":\""
                        + uniqueEmail()
                        + "\",\"fullName\":\"Bé Na\",\"role\":\"STUDENT\",\"dateOfBirth\":\""
                        + LocalDate.now(VN).minusYears(12)
                        + "\"}")
                .andExpect(status().isCreated()),
            "$.id");
    String consentPath = "/api/v1/students/" + child + "/guardian-consent";
    call(
            put(consentPath),
            admin,
            "{\"guardianName\":\"Trần Thị Mẹ\",\"relationship\":\"Mẹ\","
                + "\"guardianContact\":\"0901234567\",\"consentedOn\":\"2026-01-15\"}")
        .andExpect(status().isOk());

    anonymize(admin, request(admin, UUID.fromString(child), "ERASURE")).andExpect(status().isOk());

    call(get(consentPath), admin).andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auth_token where user_id = ?::uuid and used_at is null",
                Long.class,
                child))
        .as("token mời đang còn hạn cũng bị thu hồi")
        .isZero();
  }

  @Test
  void noEmailGoesToAnonymizedAddressFromLateEvents() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    anonymize(admin, request(admin, student.id(), "ERASURE")).andExpect(status().isOk());
    awaitListenersDone(student.id());

    // nhắc lịch đã nằm trong hàng đợi trước lúc ẩn danh hóa tới sau đó
    UUID sessionId = UUID.randomUUID();
    new TransactionTemplate(txManager)
        .executeWithoutResult(
            s ->
                publisher.publishEvent(
                    new SessionReminderDue(
                        sessionId, teacher.id(), student.id(), at(1, 9), ReminderWindow.H1)));
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(emailSender.sent())
                    .extracting(EmailMessage::to)
                    .contains(teacher.email()));
    awaitListenersDone(student.id());

    assertThat(emailSender.sent())
        .extracting(EmailMessage::to)
        .noneMatch(to -> to.endsWith("@invalid"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from notification where user_id = ?", Long.class, student.id()))
        .isZero();
  }

  @Test
  void anonymizeOnlyForReceivedErasureWithConfirmation() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);

    UUID access = request(admin, student.id(), "ACCESS");
    anonymize(admin, access)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    call(post("/api/v1/data-subject-requests/" + access + "/complete-access"), admin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    call(post("/api/v1/data-subject-requests/" + access + "/complete-access"), admin)
        .andExpect(status().isConflict());

    UUID erasure = request(admin, student.id(), "ERASURE");
    call(post("/api/v1/data-subject-requests/" + erasure + "/complete-access"), admin)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    call(
            post("/api/v1/data-subject-requests/" + erasure + "/anonymize"),
            admin,
            "{\"confirm\":false}")
        .andExpect(status().isBadRequest());
    call(post("/api/v1/data-subject-requests/" + erasure + "/reject"), admin, "{}")
        .andExpect(status().isBadRequest());
    call(
            post("/api/v1/data-subject-requests/" + erasure + "/reject"),
            admin,
            "{\"resolutionNote\":\"Không xác minh được người yêu cầu\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.resolutionNote").value("Không xác minh được người yêu cầu"));
    anonymize(admin, erasure)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

    assertThat(users.findById(student.id()).orElseThrow().isAnonymized()).isFalse();
    call(get("/api/v1/data-subject-requests?status=REJECTED&studentId=" + student.id()), admin)
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void requestsApplyOnlyToExistingStudentsAndValidDates() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    String body =
        "{\"studentId\":\"%s\",\"requesterName\":\"A\",\"type\":\"ERASURE\",\"receivedOn\":\"%s\"}";

    call(
            post("/api/v1/data-subject-requests"),
            admin,
            body.formatted(teacher.id(), LocalDate.now(VN)))
        .andExpect(status().isNotFound());
    call(
            post("/api/v1/data-subject-requests"),
            admin,
            body.formatted(UUID.randomUUID(), LocalDate.now(VN)))
        .andExpect(status().isNotFound());
    call(
            post("/api/v1/data-subject-requests"),
            admin,
            body.formatted(user(Role.STUDENT).id(), LocalDate.now(VN).plusDays(2)))
        .andExpect(status().isBadRequest());
    call(
            post("/api/v1/data-subject-requests/" + UUID.randomUUID() + "/reject"),
            admin,
            "{\"resolutionNote\":\"x\"}")
        .andExpect(status().isNotFound());
  }

  @Test
  void teacherAndStudentCannotReachPrivacyEndpoints() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    UUID requestId = request(admin, student.id(), "ERASURE");
    String base = "/api/v1/data-subject-requests";

    for (TestUser other : new TestUser[] {student, user(Role.TEACHER)}) {
      call(get(base), other).andExpect(status().isForbidden());
      call(
              post(base),
              other,
              "{\"studentId\":\""
                  + student.id()
                  + "\",\"requesterName\":\"A\",\"type\":\"ERASURE\",\"receivedOn\":\"2026-01-01\"}")
          .andExpect(status().isForbidden());
      call(post(base + "/" + requestId + "/reject"), other, "{\"resolutionNote\":\"x\"}")
          .andExpect(status().isForbidden());
      call(post(base + "/" + requestId + "/complete-access"), other)
          .andExpect(status().isForbidden());
      call(post(base + "/" + requestId + "/anonymize"), other, "{\"confirm\":true}")
          .andExpect(status().isForbidden());
    }
    assertThat(users.findById(student.id()).orElseThrow().isAnonymized()).isFalse();
  }
}
