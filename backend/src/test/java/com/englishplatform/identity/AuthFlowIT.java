package com.englishplatform.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.internal.InvitationService;
import com.englishplatform.identity.internal.PolicyConsent;
import com.englishplatform.identity.internal.PolicyConsentRepository;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAccountRepository;
import com.englishplatform.support.PostgresContainerConfig;
import com.englishplatform.support.TestClockConfig;
import com.englishplatform.support.TestClockConfig.MutableClock;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Luồng xác thực trọng yếu (constitution IV, NFR-SEC-03/04/10): đăng nhập, xoay vòng refresh, đăng
 * xuất, lời mời + đồng ý chính sách, đặt lại mật khẩu, consent gate, chặn tài khoản ngừng kích
 * hoạt, ma trận vai trò tối thiểu cho endpoint identity.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({PostgresContainerConfig.class, TestClockConfig.class})
class AuthFlowIT {

  static final String PASSWORD = "Matkhau123";
  static final String POLICY = "2026-11";

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository users;
  @Autowired PolicyConsentRepository consents;
  @Autowired InvitationService invitations;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired MutableClock clock;
  @Autowired ApplicationEvents events;

  @AfterEach
  void resetClock() {
    clock.reset();
  }

  @Test
  void wrongPasswordIsRejected() throws Exception {
    UserAccount user = activeUser(Role.TEACHER, true);
    login(user.getEmail(), "SaiMatKhau1")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void inactiveUserCannotLogin() throws Exception {
    UserAccount user = activeUser(Role.TEACHER, true);
    user.setStatus(UserStatus.INACTIVE);
    users.save(user);
    login(user.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
  }

  @Test
  void refreshTokenRotatesAndReuseRevokesAll() throws Exception {
    UserAccount user = activeUser(Role.STUDENT, true);
    Cookie first =
        refreshCookie(login(user.getEmail(), PASSWORD).andExpect(status().isOk()).andReturn());

    MvcResult rotated =
        mvc.perform(post("/api/v1/auth/refresh").cookie(first))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();
    Cookie second = refreshCookie(rotated);
    assertThat(second.getValue()).isNotEqualTo(first.getValue());

    // dùng lại token cũ sau cửa sổ ân hạn → 401 và thu hồi luôn token mới
    clock.advance(Duration.ofSeconds(31));
    mvc.perform(post("/api/v1/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
  }

  @Test
  void reuseWithinGraceWindowDoesNotLogOutOtherTabs() throws Exception {
    UserAccount user = activeUser(Role.STUDENT, true);
    Cookie first = refreshCookie(login(user.getEmail(), PASSWORD).andReturn());
    Cookie second =
        refreshCookie(
            mvc.perform(post("/api/v1/auth/refresh").cookie(first))
                .andExpect(status().isOk())
                .andReturn());

    // tab khác refresh bằng token vừa xoay: bị từ chối nhưng không thu hồi phiên của tab thắng
    mvc.perform(post("/api/v1/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/auth/refresh").cookie(second)).andExpect(status().isOk());
  }

  @Test
  void concurrentRefreshIssuesAtMostOneSession() throws Exception {
    UserAccount user = activeUser(Role.STUDENT, true);
    Cookie cookie = refreshCookie(login(user.getEmail(), PASSWORD).andReturn());
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(4);
    try {
      List<Future<Integer>> results = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  return mvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      }
      start.countDown();
      List<Integer> statuses = new ArrayList<>();
      for (Future<Integer> f : results) {
        statuses.add(f.get(30, TimeUnit.SECONDS));
      }
      assertThat(statuses).containsOnly(200, 401);
      assertThat(statuses.stream().filter(code -> code == 200).count()).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void refreshCookieHasSecureFlags() throws Exception {
    UserAccount user = activeUser(Role.STUDENT, true);
    String header =
        login(user.getEmail(), PASSWORD).andReturn().getResponse().getHeader("Set-Cookie");
    assertThat(header)
        .startsWith("refresh_token=")
        .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth");
  }

  @Test
  void passwordsAreHashedWithBcryptCost10() {
    UserAccount user = activeUser(Role.TEACHER, true);
    assertThat(users.findById(user.getId()).orElseThrow().getPasswordHash()).startsWith("$2a$10$");
  }

  @Test
  void deactivatedUserLosesAccessImmediately() throws Exception {
    UserAccount user = activeUser(Role.TEACHER, true);
    MvcResult result = login(user.getEmail(), PASSWORD).andReturn();
    String token = accessToken(result);
    Cookie cookie = refreshCookie(result);

    user.setStatus(UserStatus.INACTIVE);
    users.save(user);

    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    mvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutRevokesRefreshToken() throws Exception {
    UserAccount user = activeUser(Role.STUDENT, true);
    Cookie cookie = refreshCookie(login(user.getEmail(), PASSWORD).andReturn());
    mvc.perform(post("/api/v1/auth/logout").cookie(cookie)).andExpect(status().isNoContent());
    mvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
  }

  @Test
  void expiredAccessTokenReturnsTokenExpired() throws Exception {
    UserAccount user = activeUser(Role.ADMIN, true);
    clock.set(Instant.now().minus(Duration.ofMinutes(20)));
    String token = accessToken(login(user.getEmail(), PASSWORD).andReturn());
    clock.reset();
    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
  }

  @Test
  void acceptInviteRequiresPolicyConsentThenActivates() throws Exception {
    UserAccount user =
        users.save(
            new UserAccount(
                email(),
                "Học viên mới",
                Role.STUDENT,
                LocalDate.of(2000, 1, 1),
                UserStatus.INVITED));
    invitations.issueInvite(user);
    String token = latestInviteToken(user.getId());

    mvc.perform(
            post("/api/v1/auth/accept-invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

    mvc.perform(
            post("/api/v1/auth/accept-invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\""
                        + token
                        + "\",\"password\":\""
                        + PASSWORD
                        + "\",\"acceptedPolicyVersion\":\""
                        + POLICY
                        + "\"}"))
        .andExpect(status().isNoContent());

    assertThat(users.findById(user.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(consents.existsByUserIdAndPolicyVersion(user.getId(), POLICY)).isTrue();
    login(user.getEmail(), PASSWORD).andExpect(status().isOk());

    // token mời chỉ dùng một lần
    mvc.perform(
            post("/api/v1/auth/accept-invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\""
                        + token
                        + "\",\"password\":\""
                        + PASSWORD
                        + "\",\"acceptedPolicyVersion\":\""
                        + POLICY
                        + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void inviteExpiresAfter72Hours() throws Exception {
    UserAccount user =
        users.save(
            new UserAccount(
                email(), "Học viên", Role.STUDENT, LocalDate.of(2000, 1, 1), UserStatus.INVITED));
    invitations.issueInvite(user);
    String token = latestInviteToken(user.getId());

    clock.advance(Duration.ofHours(72).plusMinutes(1));
    acceptInvite(token, PASSWORD).andExpect(status().isBadRequest());
    assertThat(users.findById(user.getId()).orElseThrow().getStatus())
        .isEqualTo(UserStatus.INVITED);
  }

  @Test
  void weakOrTooLongPasswordIsRejected() throws Exception {
    UserAccount user =
        users.save(
            new UserAccount(
                email(), "Học viên", Role.STUDENT, LocalDate.of(2000, 1, 1), UserStatus.INVITED));
    invitations.issueInvite(user);
    String token = latestInviteToken(user.getId());

    acceptInvite(token, "chiCoChu")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("password"));
    acceptInvite(token, "a1" + "x".repeat(71))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    // mật khẩu > 72 byte khi đăng nhập: 401 thường, không phải 500
    UserAccount active = activeUser(Role.TEACHER, true);
    login(active.getEmail(), "a1" + "x".repeat(100)).andExpect(status().isUnauthorized());
  }

  @Test
  void underageStudentNeedsGuardianConsent() throws Exception {
    LocalDate dob = LocalDate.now().minusYears(12);
    UserAccount user =
        users.save(new UserAccount(email(), "Bé Na", Role.STUDENT, dob, UserStatus.INVITED));
    invitations.issueInvite(user);
    String token = latestInviteToken(user.getId());
    mvc.perform(
            post("/api/v1/auth/accept-invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"token\":\""
                        + token
                        + "\",\"password\":\""
                        + PASSWORD
                        + "\",\"acceptedPolicyVersion\":\""
                        + POLICY
                        + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("GUARDIAN_CONSENT_REQUIRED"));
  }

  @Test
  void passwordResetTokenExpiresAfter30Minutes() throws Exception {
    UserAccount user = activeUser(Role.TEACHER, true);
    mvc.perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + user.getEmail() + "\"}"))
        .andExpect(status().isAccepted());
    String token =
        events.stream(PasswordResetRequested.class)
            .filter(e -> e.userId().equals(user.getId()))
            .reduce((a, b) -> b)
            .orElseThrow()
            .resetToken();

    clock.advance(Duration.ofMinutes(31));
    mvc.perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"MatKhauMoi9\"}"))
        .andExpect(status().isBadRequest());

    clock.reset();
    mvc.perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"MatKhauMoi9\"}"))
        .andExpect(status().isNoContent());
    login(user.getEmail(), "MatKhauMoi9").andExpect(status().isOk());
  }

  @Test
  void forgotPasswordAlwaysReturns202() throws Exception {
    mvc.perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"khong-ton-tai@demo.local\"}"))
        .andExpect(status().isAccepted());
  }

  @Test
  void consentGateBlocksUntilCurrentPolicyAccepted() throws Exception {
    UserAccount user = activeUser(Role.STUDENT, false);
    String token =
        accessToken(login(user.getEmail(), PASSWORD).andExpect(status().isOk()).andReturn());

    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consentRequired").value(true))
        .andExpect(jsonPath("$.currentPolicyVersion").value(POLICY));
    mvc.perform(
            patch("/api/v1/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0900000000\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));

    mvc.perform(
            post("/api/v1/me/consents")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"policyVersion\":\"" + POLICY + "\"}"))
        .andExpect(status().isNoContent());
    mvc.perform(
            patch("/api/v1/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0900000000\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consentRequired").value(false));
  }

  @Test
  void policyVersionChangeRequiresNewConsentAndKeepsHistory() throws Exception {
    UserAccount user = activeUser(Role.STUDENT, false);
    consents.save(new PolicyConsent(user.getId(), "2025-01", Instant.now()));
    String token = accessToken(login(user.getEmail(), PASSWORD).andReturn());

    mvc.perform(
            patch("/api/v1/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0900000001\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONSENT_REQUIRED"));
    mvc.perform(
            post("/api/v1/me/consents")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"policyVersion\":\"" + POLICY + "\"}"))
        .andExpect(status().isNoContent());

    assertThat(consents.existsByUserIdAndPolicyVersion(user.getId(), "2025-01")).isTrue();
    assertThat(consents.existsByUserIdAndPolicyVersion(user.getId(), POLICY)).isTrue();
  }

  /** Ma trận vai trò tối thiểu cho endpoint identity hiện có (bản đầy đủ: T140). */
  @Test
  void identityEndpointsRequireAuthentication() throws Exception {
    mvc.perform(get("/api/v1/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    mvc.perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/me/consents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"policyVersion\":\"" + POLICY + "\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/auth/policy")).andExpect(status().isOk());
  }

  @Test
  void everyRoleCanReadOwnProfile() throws Exception {
    for (Role role : Role.values()) {
      UserAccount user = activeUser(role, true);
      String token = accessToken(login(user.getEmail(), PASSWORD).andReturn());
      mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(user.getId().toString()))
          .andExpect(jsonPath("$.role").value(role.name()));
    }
  }

  @Test
  void frameworkClientErrorsAreNot500() throws Exception {
    mvc.perform(post("/api/v1/auth/login").contentType(MediaType.TEXT_PLAIN).content("x"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    mvc.perform(get("/api/v1/auth/login"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  // ---- helpers ----

  private UserAccount activeUser(Role role, boolean consented) {
    LocalDate dob = role == Role.STUDENT ? LocalDate.of(1998, 5, 5) : null;
    UserAccount user = new UserAccount(email(), "Người dùng thử", role, dob, UserStatus.INVITED);
    user.activate(passwordEncoder.encode(PASSWORD));
    users.save(user);
    if (consented) {
      consents.save(new PolicyConsent(user.getId(), POLICY, Instant.now()));
    }
    return user;
  }

  private String latestInviteToken(UUID userId) {
    return events.stream(UserInvited.class)
        .filter(e -> e.userId().equals(userId))
        .reduce((a, b) -> b)
        .orElseThrow()
        .inviteToken();
  }

  private org.springframework.test.web.servlet.ResultActions acceptInvite(
      String token, String password) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/accept-invite")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"token\":\""
                    + token
                    + "\",\"password\":\""
                    + password
                    + "\",\"acceptedPolicyVersion\":\""
                    + POLICY
                    + "\"}"));
  }

  private org.springframework.test.web.servlet.ResultActions login(String email, String password)
      throws Exception {
    return mvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
  }

  private static Cookie refreshCookie(MvcResult result) {
    Cookie cookie = result.getResponse().getCookie("refresh_token");
    assertThat(cookie).isNotNull();
    assertThat(cookie.isHttpOnly()).isTrue();
    return cookie;
  }

  private static String accessToken(MvcResult result) throws Exception {
    String body = result.getResponse().getContentAsString();
    return com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");
  }

  private static String email() {
    return "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
  }
}
