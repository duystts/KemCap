package com.englishplatform.identity.internal.web;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.internal.AuthService;
import com.englishplatform.identity.internal.ConsentService;
import com.englishplatform.identity.internal.InvitationService;
import com.englishplatform.identity.internal.PasswordResetService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/auth/*} — contracts/rest-api.md nhóm identity. */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

  static final String REFRESH_COOKIE = "refresh_token";
  static final String COOKIE_PATH = "/api/v1/auth";

  private final AuthService authService;
  private final InvitationService invitationService;
  private final PasswordResetService passwordResetService;
  private final ConsentService consentService;

  AuthController(
      AuthService authService,
      InvitationService invitationService,
      PasswordResetService passwordResetService,
      ConsentService consentService) {
    this.authService = authService;
    this.invitationService = invitationService;
    this.passwordResetService = passwordResetService;
    this.consentService = consentService;
  }

  record PolicyResponse(String currentPolicyVersion) {}

  /** Public: trang chấp nhận lời mời cần phiên bản chính sách hiện hành trước khi đăng nhập. */
  @GetMapping("/policy")
  PolicyResponse policy() {
    return new PolicyResponse(consentService.currentPolicyVersion());
  }

  record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  record UserSummary(UUID id, String email, String fullName, Role role, String timezone) {}

  record TokenResponse(String accessToken, UserSummary user) {}

  record AcceptInviteRequest(
      @NotBlank String token, @NotBlank String password, String acceptedPolicyVersion) {}

  record ForgotPasswordRequest(String email) {}

  record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword) {}

  @PostMapping("/login")
  TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    return respond(authService.login(request.email(), request.password()), response);
  }

  @PostMapping("/refresh")
  TokenResponse refresh(
      @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
      HttpServletResponse response) {
    return respond(authService.refresh(refreshToken), response);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void logout(
      @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
      HttpServletResponse response) {
    authService.logout(refreshToken);
    setRefreshCookie(response, "", Duration.ZERO);
  }

  @PostMapping("/accept-invite")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
    invitationService.acceptInvite(
        request.token(), request.password(), request.acceptedPolicyVersion());
  }

  /** Luôn 202 để không lộ email có tồn tại hay không (NFR-SEC-10). */
  @PostMapping("/forgot-password")
  @ResponseStatus(HttpStatus.ACCEPTED)
  void forgotPassword(@RequestBody ForgotPasswordRequest request) {
    passwordResetService.requestReset(request.email());
  }

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.token(), request.newPassword());
  }

  private TokenResponse respond(AuthService.Session session, HttpServletResponse response) {
    setRefreshCookie(response, session.refreshToken(), authService.refreshTokenLifetime());
    var u = session.user();
    return new TokenResponse(
        session.accessToken(),
        new UserSummary(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getTimezone()));
  }

  private static void setRefreshCookie(
      HttpServletResponse response, String value, Duration maxAge) {
    ResponseCookie cookie =
        ResponseCookie.from(REFRESH_COOKIE, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path(COOKIE_PATH)
            .maxAge(maxAge)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
