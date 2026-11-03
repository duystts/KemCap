package com.englishplatform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.support.ApiTestSupport;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

/** Đồng ý của người giám hộ (FR-030): dưới 16 tuổi chưa có đồng ý thì không có lời mời. */
@RecordApplicationEvents
class GuardianConsentIT extends ApiTestSupport {

  private static final String CONSENT =
      "{\"guardianName\":\"Trần Thị Mẹ\",\"relationship\":\"Mẹ\","
          + "\"guardianContact\":\"0901234567\",\"consentedOn\":\"2026-01-15\"}";

  @Autowired ApplicationEvents events;
  @Autowired JdbcTemplate jdbc;

  private UUID createStudent(TestUser admin, LocalDate dob) throws Exception {
    return UUID.fromString(
        read(
            call(
                    post("/api/v1/users"),
                    admin,
                    "{\"email\":\""
                        + uniqueEmail()
                        + "\",\"fullName\":\"Bé Na\",\"role\":\"STUDENT\",\"dateOfBirth\":\""
                        + dob
                        + "\"}")
                .andExpect(status().isCreated()),
            "$.id"));
  }

  private long invites(UUID userId) {
    return events.stream(UserInvited.class).filter(e -> e.userId().equals(userId)).count();
  }

  private ResultActions acceptInvite(String token) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/accept-invite")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"token\":\""
                    + token
                    + "\",\"password\":\""
                    + PASSWORD
                    + "\",\"acceptedPolicyVersion\":\""
                    + privacy.policyVersion()
                    + "\"}"));
  }

  @Test
  void underageStudentIsInvitedOnlyAfterGuardianConsent() throws Exception {
    TestUser admin = user(Role.ADMIN);
    UUID studentId = createStudent(admin, LocalDate.now().minusYears(12));

    call(get("/api/v1/users/" + studentId), admin)
        .andExpect(jsonPath("$.status").value("AWAITING_GUARDIAN_CONSENT"));
    assertThat(invites(studentId)).isZero();
    call(get("/api/v1/students/" + studentId + "/guardian-consent"), admin)
        .andExpect(status().isNotFound());

    call(put("/api/v1/students/" + studentId + "/guardian-consent"), admin, CONSENT)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.guardianName").value("Trần Thị Mẹ"))
        .andExpect(jsonPath("$.recordedBy").value(admin.id().toString()));

    call(get("/api/v1/users/" + studentId), admin).andExpect(jsonPath("$.status").value("INVITED"));
    assertThat(invites(studentId)).isEqualTo(1);
    call(get("/api/v1/students/" + studentId + "/guardian-consent"), admin)
        .andExpect(jsonPath("$.relationship").value("Mẹ"))
        .andExpect(jsonPath("$.consentedOn").value("2026-01-15"));

    // sửa thông tin đồng ý: không gửi lời mời lần nữa
    call(
            put("/api/v1/students/" + studentId + "/guardian-consent"),
            admin,
            CONSENT.replace("Mẹ\"", "Bố\""))
        .andExpect(status().isOk());
    assertThat(invites(studentId)).isEqualTo(1);

    String token =
        events.stream(UserInvited.class)
            .filter(e -> e.userId().equals(studentId))
            .findFirst()
            .orElseThrow()
            .inviteToken();
    acceptInvite(token).andExpect(status().is2xxSuccessful());
    assertThat(users.findById(studentId).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void invitedUnderageStudentWithoutConsentCannotAcceptInvite() throws Exception {
    TestUser admin = user(Role.ADMIN);
    UUID studentId = createStudent(admin, LocalDate.now().minusYears(12));
    call(put("/api/v1/students/" + studentId + "/guardian-consent"), admin, CONSENT)
        .andExpect(status().isOk());
    String token =
        events.stream(UserInvited.class)
            .filter(e -> e.userId().equals(studentId))
            .findFirst()
            .orElseThrow()
            .inviteToken();
    // lớp chặn cuối vẫn giữ khi lời mời đã phát mà đồng ý không còn
    jdbc.update("delete from guardian_consent where student_id = ?", studentId);

    acceptInvite(token)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("GUARDIAN_CONSENT_REQUIRED"));
  }

  /** F1: ngừng rồi kích hoạt lại không được vòng qua bước đồng ý của người giám hộ. */
  @Test
  void reactivatingUnderageStudentWithoutConsentGoesBackToAwaiting() throws Exception {
    TestUser admin = user(Role.ADMIN);
    UUID studentId = createStudent(admin, LocalDate.now().minusYears(12));

    call(patch("/api/v1/users/" + studentId + "/status"), admin, "{\"status\":\"INACTIVE\"}")
        .andExpect(jsonPath("$.status").value("INACTIVE"));
    call(patch("/api/v1/users/" + studentId + "/status"), admin, "{\"status\":\"ACTIVE\"}")
        .andExpect(jsonPath("$.status").value("AWAITING_GUARDIAN_CONSENT"));
    call(post("/api/v1/users/" + studentId + "/resend-invite"), admin)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("GUARDIAN_CONSENT_REQUIRED"));
    assertThat(invites(studentId)).isZero();

    call(put("/api/v1/students/" + studentId + "/guardian-consent"), admin, CONSENT)
        .andExpect(status().isOk());
    assertThat(invites(studentId)).isEqualTo(1);
    assertThat(users.findById(studentId).orElseThrow().getStatus()).isEqualTo(UserStatus.INVITED);
  }

  /** F1: hồ sơ đã kẹt ở {@code INVITED} thiếu đồng ý: không gửi mời được, nhưng gỡ được. */
  @Test
  void invitedUnderageStudentWithoutConsentGetsNoInviteUntilConsentRecorded() throws Exception {
    UserAccount student =
        users.save(
            new UserAccount(
                uniqueEmail(),
                "Bé Bo",
                Role.STUDENT,
                LocalDate.now().minusYears(12),
                UserStatus.INVITED));
    TestUser admin = user(Role.ADMIN);

    call(post("/api/v1/users/" + student.getId() + "/resend-invite"), admin)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("GUARDIAN_CONSENT_REQUIRED"));
    assertThat(invites(student.getId())).isZero();

    call(put("/api/v1/students/" + student.getId() + "/guardian-consent"), admin, CONSENT)
        .andExpect(status().isOk());
    assertThat(invites(student.getId())).isEqualTo(1);
  }

  @Test
  void consentRequiresAllFieldsAndNoFutureDate() throws Exception {
    TestUser admin = user(Role.ADMIN);
    UUID studentId = createStudent(admin, LocalDate.now().minusYears(12));
    String path = "/api/v1/students/" + studentId + "/guardian-consent";

    call(put(path), admin, CONSENT.replace("\"guardianName\":\"Trần Thị Mẹ\",", ""))
        .andExpect(status().isBadRequest());
    call(put(path), admin, CONSENT.replace("Mẹ\"", " \"")).andExpect(status().isBadRequest());
    call(put(path), admin, CONSENT.replace("2026-01-15", LocalDate.now().plusDays(2).toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

    assertThat(users.findById(studentId).orElseThrow().getStatus())
        .isEqualTo(UserStatus.AWAITING_GUARDIAN_CONSENT);
    assertThat(invites(studentId)).isZero();
  }

  @Test
  void consentOnlyAppliesToStudentAwaitingIt() throws Exception {
    TestUser admin = user(Role.ADMIN);
    UUID adult = createStudent(admin, LocalDate.now().minusYears(20));
    call(put("/api/v1/students/" + adult + "/guardian-consent"), admin, CONSENT)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

    TestUser teacher = user(Role.TEACHER);
    call(put("/api/v1/students/" + teacher.id() + "/guardian-consent"), admin, CONSENT)
        .andExpect(status().isNotFound());
    call(put("/api/v1/students/" + UUID.randomUUID() + "/guardian-consent"), admin, CONSENT)
        .andExpect(status().isNotFound());
  }

  @Test
  void onlyAdminReachesGuardianConsent() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    UUID child = createStudent(admin, LocalDate.now().minusYears(12));
    String path = "/api/v1/students/" + child + "/guardian-consent";

    for (TestUser other : new TestUser[] {student, user(Role.TEACHER)}) {
      call(get(path), other).andExpect(status().isForbidden());
      call(put(path), other, CONSENT).andExpect(status().isForbidden());
    }
    assertThat(invites(child)).isZero();
  }
}
