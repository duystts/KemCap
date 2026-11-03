package com.englishplatform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.notification.EmailSender.EmailMessage;
import com.englishplatform.notification.internal.FakeEmailSender;
import com.englishplatform.support.ApiTestSupport;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** Log của luồng tài khoản không chứa mật khẩu và token thô (NFR-PRIV-02). */
@RecordApplicationEvents
@ExtendWith(OutputCaptureExtension.class)
class LogPrivacyIT extends ApiTestSupport {

  private static final String NEW_PASSWORD = "MatkhauMoi456";

  @Autowired ApplicationEvents events;
  @Autowired FakeEmailSender emailSender;

  private void postJson(String path, String json, int expected) throws Exception {
    mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().is(expected));
  }

  private void awaitEmailTo(String email, int count) {
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(emailSender.sent())
                    .extracting(EmailMessage::to)
                    .filteredOn(email::equals)
                    .hasSizeGreaterThanOrEqualTo(count));
  }

  @Test
  void accountFlowsDoNotLogPasswordsOrTokens(CapturedOutput output) throws Exception {
    TestUser admin = user(Role.ADMIN);
    String email = uniqueEmail();
    call(
            post("/api/v1/users"),
            admin,
            "{\"email\":\"" + email + "\",\"fullName\":\"Giáo viên\",\"role\":\"TEACHER\"}")
        .andExpect(status().isCreated());
    String inviteToken =
        events.stream(UserInvited.class)
            .filter(e -> e.email().equals(email))
            .findFirst()
            .orElseThrow()
            .inviteToken();
    awaitEmailTo(email, 1);

    postJson(
        "/api/v1/auth/accept-invite",
        "{\"token\":\""
            + inviteToken
            + "\",\"password\":\""
            + PASSWORD
            + "\",\"acceptedPolicyVersion\":\""
            + privacy.policyVersion()
            + "\"}",
        204);
    postJson(
        "/api/v1/auth/login",
        "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
        200);
    postJson("/api/v1/auth/forgot-password", "{\"email\":\"" + email + "\"}", 202);
    String resetToken =
        events.stream(PasswordResetRequested.class)
            .filter(e -> e.email().equals(email))
            .findFirst()
            .orElseThrow()
            .resetToken();
    awaitEmailTo(email, 2);
    postJson(
        "/api/v1/auth/reset-password",
        "{\"token\":\"" + resetToken + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}",
        204);
    // sai mật khẩu cũng không được ghi mật khẩu đã nhập vào log
    postJson(
        "/api/v1/auth/login",
        "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}",
        401);

    assertThat(output.getAll()).doesNotContain(PASSWORD, NEW_PASSWORD, inviteToken, resetToken);
  }
}
