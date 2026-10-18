package com.englishplatform.identity.internal;

import com.englishplatform.identity.PasswordResetRequested;
import com.englishplatform.shared.web.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quên/đặt lại mật khẩu: token hạn 30 phút, dùng một lần; không lộ email có tồn tại hay không. */
@Service
public class PasswordResetService {

  static final Duration RESET_TTL = Duration.ofMinutes(30);

  private final UserAccountRepository users;
  private final AuthTokenRepository tokens;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  PasswordResetService(
      UserAccountRepository users,
      AuthTokenRepository tokens,
      PasswordEncoder passwordEncoder,
      ApplicationEventPublisher events,
      Clock clock) {
    this.users = users;
    this.tokens = tokens;
    this.passwordEncoder = passwordEncoder;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public void requestReset(String email) {
    if (email == null || email.isBlank()) {
      return;
    }
    users
        .findByEmailIgnoreCase(email.trim())
        .filter(UserAccount::isActive)
        .ifPresent(this::issueResetToken);
  }

  @Transactional
  public void resetPassword(String token, String newPassword) {
    PasswordPolicy.check("newPassword", newPassword);
    Instant now = clock.instant();
    AuthToken reset =
        tokens
            .findByTokenHashAndType(
                TokenHasher.hash(token == null ? "" : token), AuthToken.Type.PASSWORD_RESET)
            .filter(t -> t.isUsable(now))
            .orElseThrow(PasswordResetService::invalidToken);
    UserAccount user =
        users
            .findById(reset.getUserId())
            .filter(UserAccount::isActive)
            .orElseThrow(PasswordResetService::invalidToken);
    reset.markUsed(now);
    user.changePassword(passwordEncoder.encode(newPassword));
    tokens.revokeAll(user.getId(), AuthToken.Type.REFRESH, now);
  }

  private void issueResetToken(UserAccount user) {
    Instant now = clock.instant();
    tokens.revokeAll(user.getId(), AuthToken.Type.PASSWORD_RESET, now);
    String token = TokenHasher.newToken();
    AuthToken reset =
        tokens.save(
            new AuthToken(
                user.getId(),
                AuthToken.Type.PASSWORD_RESET,
                TokenHasher.hash(token),
                now.plus(RESET_TTL)));
    events.publishEvent(
        new PasswordResetRequested(user.getId(), user.getEmail(), reset.getId(), token));
  }

  private static BusinessException invalidToken() {
    return BusinessException.validation(
        "token", "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
  }
}
