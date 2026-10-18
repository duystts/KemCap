package com.englishplatform.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.internal.profile.StudentProfileRepository;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.identity.UserInvited;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.notification.internal.FakeEmailSender;
import com.englishplatform.support.ApiTestSupport;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Luồng giáo vụ qua API thật — quickstart.md bước 1–3 (chuyển từ cổng U1) và các DoD còn lại của
 * U2.
 */
@RecordApplicationEvents
class CatalogFlowIT extends ApiTestSupport {

  @Autowired ApplicationEvents events;
  @Autowired FakeEmailSender emailSender;
  @Autowired StudentProfileRepository studentProfiles;
  @Autowired ApplicationEventPublisher publisher;
  @Autowired PlatformTransactionManager txManager;
  @Autowired JdbcTemplate jdbc;

  @Test
  void quickstartSteps1To3() throws Exception {
    TestUser admin = user(Role.ADMIN);

    // Bước 1: tạo giáo viên + học viên → email mời → đặt mật khẩu → đăng nhập
    String teacherEmail = uniqueEmail();
    String studentEmail = uniqueEmail();
    UUID teacherId = createUser(admin, teacherEmail, "Giáo viên mới", "TEACHER", null);
    UUID studentId = createUser(admin, studentEmail, "Học viên mới", "STUDENT", "2001-05-05");
    await()
        .atMost(Duration.ofSeconds(10))
        .until(
            () ->
                emailSender.sent().stream()
                        .filter(m -> m.to().equals(teacherEmail) || m.to().equals(studentEmail))
                        .count()
                    == 2);
    acceptInvite(teacherId);
    acceptInvite(studentId);
    TestUser teacher = new TestUser(teacherId, teacherEmail, Role.TEACHER, login(teacherEmail));
    TestUser student = new TestUser(studentId, studentEmail, Role.STUDENT, login(studentEmail));

    // Bước 2: gói "Giao tiếp 12 buổi × 45 phút, 90 ngày", ghi danh, phân công
    String courseId =
        read(
            call(
                    post("/api/v1/courses"),
                    admin,
                    "{\"name\":\"Giao tiếp " + UUID.randomUUID().toString().substring(0, 6) + "\"}")
                .andExpect(status().isCreated()),
            "$.id");
    String packageId =
        read(
            call(
                    post("/api/v1/packages"),
                    admin,
                    "{\"courseId\":\""
                        + courseId
                        + "\",\"name\":\"Giao tiếp 12 buổi × 45 phút, 90 ngày\",\"sessionCount\":12,"
                        + "\"sessionMinutes\":45,\"validityDays\":90,\"priceVnd\":3000000}")
                .andExpect(status().isCreated()),
            "$.id");
    String enrollmentId =
        read(
            call(
                    post("/api/v1/enrollments"),
                    admin,
                    "{\"studentId\":\""
                        + studentId
                        + "\",\"packageId\":\""
                        + packageId
                        + "\",\"teacherId\":\""
                        + teacherId
                        + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.sessionsRemaining").value(12)),
            "$.id");
    call(get("/api/v1/students"), teacher)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(studentId.toString()));

    // Bước 3: thanh toán 50% → ACTIVE, còn 12 buổi, công nợ 50%
    String paymentsUrl = "/api/v1/enrollments/" + enrollmentId + "/payments";
    // công nợ có ngay trong transaction tạo ghi danh — không cần chờ (R1 verify U2)
    call(
            post(paymentsUrl),
            admin,
            "{\"amountVnd\":1500000,\"paidAt\":\""
                + LocalDate.now()
                + "\",\"method\":\"BANK_TRANSFER\"}")
        .andExpect(status().isCreated());
    call(get("/api/v1/enrollments/" + enrollmentId), student)
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.sessionsRemaining").value(12))
        .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    call(get(paymentsUrl), student)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.debtVnd").value(1500000))
        .andExpect(jsonPath("$.payments.length()").value(1));
    // giáo viên không xem học phí
    call(get("/api/v1/enrollments/" + enrollmentId), teacher)
        .andExpect(jsonPath("$.priceVnd").doesNotExist());
  }

  @Test
  void underageStudentWaitsForGuardianConsentWithoutInvite() throws Exception {
    TestUser admin = user(Role.ADMIN);
    String dob = LocalDate.now().minusYears(12).toString();
    UUID id = createUser(admin, uniqueEmail(), "Bé Na", "STUDENT", dob);
    assertThat(users.findById(id).orElseThrow().getStatus())
        .isEqualTo(UserStatus.AWAITING_GUARDIAN_CONSENT);
    assertThat(events.stream(UserInvited.class).filter(e -> e.userId().equals(id))).isEmpty();
  }

  @Test
  void createUserValidation() throws Exception {
    TestUser admin = user(Role.ADMIN);
    call(
            post("/api/v1/users"),
            admin,
            "{\"email\":\"" + admin.email() + "\",\"fullName\":\"Trùng\",\"role\":\"TEACHER\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    call(
            post("/api/v1/users"),
            admin,
            "{\"email\":\"" + uniqueEmail() + "\",\"fullName\":\"HV\",\"role\":\"STUDENT\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("dateOfBirth"));
    TestUser teacher = user(Role.TEACHER);
    call(get("/api/v1/users"), teacher).andExpect(status().isForbidden());
  }

  @Test
  void deactivationRevokesSessionsAndReactivationRestores() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    Cookie refresh =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\""
                            + student.email()
                            + "\",\"password\":\""
                            + PASSWORD
                            + "\"}"))
            .andReturn()
            .getResponse()
            .getCookie("refresh_token");

    call(patch("/api/v1/users/" + student.id() + "/status"), admin, "{\"status\":\"INACTIVE\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
    call(get("/api/v1/me"), student).andExpect(status().isUnauthorized());
    call(patch("/api/v1/users/" + student.id() + "/status"), admin, "{\"status\":\"ACTIVE\"}")
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    // refresh token cũ đã bị thu hồi lúc ngừng kích hoạt (R9)
    mvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
    call(patch("/api/v1/users/" + admin.id() + "/status"), admin, "{\"status\":\"INACTIVE\"}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void anonymizedUserStatusCannotChange() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    jdbc.update("update user_account set anonymized_at = now() where id = ?", student.id());
    call(patch("/api/v1/users/" + student.id() + "/status"), admin, "{\"status\":\"INACTIVE\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ANONYMIZED"));
  }

  @Test
  void packagePriceChangeDoesNotAffectExistingEnrollment() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    String courseId =
        read(
            call(
                post("/api/v1/courses"),
                admin,
                "{\"name\":\"IELTS " + UUID.randomUUID().toString().substring(0, 6) + "\"}"),
            "$.id");
    String packageJson =
        "\"name\":\"IELTS 24\",\"sessionCount\":24,\"sessionMinutes\":60,\"validityDays\":90";
    String packageId =
        read(
            call(
                post("/api/v1/packages"),
                admin,
                "{\"courseId\":\"" + courseId + "\"," + packageJson + ",\"priceVnd\":7200000}"),
            "$.id");
    String enrollmentId =
        read(
            call(
                post("/api/v1/enrollments"),
                admin,
                "{\"studentId\":\""
                    + student.id()
                    + "\",\"packageId\":\""
                    + packageId
                    + "\",\"teacherId\":\""
                    + teacher.id()
                    + "\",\"priceVnd\":6500000}"),
            "$.id");
    call(
            put("/api/v1/packages/" + packageId),
            admin,
            "{" + packageJson + ",\"priceVnd\":9000000,\"status\":\"ACTIVE\"}")
        .andExpect(jsonPath("$.priceVnd").value(9000000));
    call(get("/api/v1/enrollments/" + enrollmentId), admin)
        .andExpect(jsonPath("$.priceVnd").value(6500000));
    call(
            put("/api/v1/packages/" + packageId),
            admin,
            "{" + packageJson + ",\"priceVnd\":9000000,\"status\":\"INACTIVE\"}")
        .andExpect(status().isOk());
    call(
            post("/api/v1/enrollments"),
            admin,
            "{\"studentId\":\""
                + student.id()
                + "\",\"packageId\":\""
                + packageId
                + "\",\"teacherId\":\""
                + teacher.id()
                + "\"}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void extendRequiresActiveTeacherAndTeacherDoesNotSeePrices() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    String courseId =
        read(
            call(
                post("/api/v1/courses"),
                admin,
                "{\"name\":\"Khóa " + UUID.randomUUID().toString().substring(0, 6) + "\"}"),
            "$.id");
    String packageId =
        read(
            call(
                post("/api/v1/packages"),
                admin,
                "{\"courseId\":\""
                    + courseId
                    + "\",\"name\":\"Gói\",\"sessionCount\":4,\"sessionMinutes\":45,"
                    + "\"validityDays\":30,\"priceVnd\":1000000}"),
            "$.id");
    call(get("/api/v1/packages?courseId=" + courseId), teacher)
        .andExpect(jsonPath("$[0].priceVnd").doesNotExist());
    call(get("/api/v1/packages?courseId=" + courseId), student)
        .andExpect(jsonPath("$[0].priceVnd").value(1000000));

    String enrollmentId =
        read(
            call(
                post("/api/v1/enrollments"),
                admin,
                "{\"studentId\":\""
                    + student.id()
                    + "\",\"packageId\":\""
                    + packageId
                    + "\",\"teacherId\":\""
                    + teacher.id()
                    + "\"}"),
            "$.id");
    call(
            post("/api/v1/enrollments/" + enrollmentId + "/payments"),
            admin,
            "{\"amountVnd\":1000000,\"paidAt\":\"" + LocalDate.now() + "\",\"method\":\"CASH\"}")
        .andExpect(status().isCreated());
    jdbc.update("update user_account set status = 'INACTIVE' where id = ?", teacher.id());
    call(
            post("/api/v1/enrollments/" + enrollmentId + "/extend"),
            admin,
            "{\"newExpiresAt\":\"" + java.time.Instant.now().plus(Duration.ofDays(60)) + "\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("teacherId"));
  }

  @Test
  void paymentsCannotBeEditedOrDeleted() throws Exception {
    TestUser admin = user(Role.ADMIN);
    String url = "/api/v1/enrollments/" + UUID.randomUUID() + "/payments";
    call(put(url), admin, "{}").andExpect(status().isMethodNotAllowed());
    call(delete(url), admin).andExpect(status().isMethodNotAllowed());
  }

  @Test
  void anonymizationListenerIsIdempotent() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    call(
            put("/api/v1/students/" + student.id() + "/profile"),
            admin,
            "{\"selfReportedLevel\":\"A2\",\"occupation\":\"Sinh viên\",\"notes\":\"Hay đi muộn\"}")
        .andExpect(status().isOk());
    for (int i = 0; i < 2; i++) {
      new TransactionTemplate(txManager)
          .executeWithoutResult(
              s -> publisher.publishEvent(new StudentAnonymized(student.id(), UUID.randomUUID())));
    }
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(studentProfiles.findById(student.id()))
                    .get()
                    .satisfies(
                        p -> {
                          assertThat(p.getNotes()).isNull();
                          assertThat(p.getOccupation()).isNull();
                          assertThat(p.getSelfReportedLevel()).isNull();
                        }));
  }

  // ---- helpers ----

  private UUID createUser(TestUser admin, String email, String name, String role, String dob)
      throws Exception {
    String json =
        "{\"email\":\""
            + email
            + "\",\"fullName\":\""
            + name
            + "\",\"role\":\""
            + role
            + "\""
            + (dob == null ? "" : ",\"dateOfBirth\":\"" + dob + "\"")
            + "}";
    return UUID.fromString(
        read(call(post("/api/v1/users"), admin, json).andExpect(status().isCreated()), "$.id"));
  }

  private void acceptInvite(UUID userId) throws Exception {
    String token =
        events.stream(UserInvited.class)
            .filter(e -> e.userId().equals(userId))
            .reduce((a, b) -> b)
            .orElseThrow()
            .inviteToken();
    mvc.perform(
            post("/api/v1/auth/accept-invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\""
                        + token
                        + "\",\"password\":\""
                        + PASSWORD
                        + "\",\"acceptedPolicyVersion\":\""
                        + privacy.policyVersion()
                        + "\"}"))
        .andExpect(status().isNoContent());
  }
}
