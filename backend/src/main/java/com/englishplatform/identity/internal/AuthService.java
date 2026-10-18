package com.englishplatform.identity.internal;

import com.englishplatform.shared.security.SecurityProperties;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng nhập, xoay vòng refresh token, đăng xuất. Refresh token 32 byte ngẫu nhiên, lưu SHA-256, hạn
 * 7 ngày; dùng lại token cũ thì thu hồi mọi refresh token của người dùng (NFR-SEC-04). Token vừa
 * xoay trong {@link #REUSE_GRACE} (nhiều tab refresh cùng lúc) chỉ trả 401, không thu hồi.
 */
@Service
public class AuthService {

  static final Duration REUSE_GRACE = Duration.ofSeconds(30);

  /** BCrypt giới hạn 72 byte. */
  static final int MAX_PASSWORD_BYTES = 72;

  private final UserAccountRepository users;
  private final AuthTokenRepository tokens;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenIssuer accessTokens;
  private final SecurityProperties properties;
  private final Clock clock;

  /** So khớp giả khi không có hash thật, để thời gian phản hồi không lộ email tồn tại. */
  private final String dummyHash;

  AuthService(
      UserAccountRepository users,
      AuthTokenRepository tokens,
      PasswordEncoder passwordEncoder,
      AccessTokenIssuer accessTokens,
      SecurityProperties properties,
      Clock clock) {
    this.users = users;
    this.tokens = tokens;
    this.passwordEncoder = passwordEncoder;
    this.accessTokens = accessTokens;
    this.properties = properties;
    this.clock = clock;
    this.dummyHash = passwordEncoder.encode(TokenHasher.newToken());
  }

  public record Session(String accessToken, String refreshToken, UserAccount user) {}

  @Transactional
  public Session login(String email, String password) {
    Optional<UserAccount> found = users.findByEmailIgnoreCase(email == null ? "" : email.trim());
    String hash = found.map(UserAccount::getPasswordHash).orElse(null);
    // luôn chạy BCrypt đúng một lần, kể cả khi email không tồn tại (NFR-SEC-10)
    boolean matches = passwordEncoder.matches(candidate(password), hash != null ? hash : dummyHash);
    UserAccount user =
        found
            .filter(u -> hash != null && matches && u.isActive())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.UNAUTHENTICATED, "Email hoặc mật khẩu không đúng"));
    user.recordLogin(clock.instant());
    return newSession(user);
  }

  @Transactional(noRollbackFor = BusinessException.class)
  public Session refresh(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED);
    }
    Instant now = clock.instant();
    AuthToken token =
        tokens
            .findByTokenHashAndType(TokenHasher.hash(refreshToken), AuthToken.Type.REFRESH)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    if (token.getUsedAt() != null) {
      // refresh token bị dùng lại sau cửa sổ ân hạn: coi như lộ, thu hồi toàn bộ
      if (!token.getUsedAt().isAfter(now.minus(REUSE_GRACE))) {
        tokens.revokeAll(token.getUserId(), AuthToken.Type.REFRESH, now);
      }
      throw new BusinessException(ErrorCode.UNAUTHENTICATED);
    }
    if (!token.isUsable(now)) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED);
    }
    UserAccount user =
        users
            .findById(token.getUserId())
            .filter(UserAccount::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    // update có điều kiện: hai request đồng thời cùng token chỉ một bên nhận phiên mới
    if (tokens.markUsedIfUnused(token.getId(), now) != 1) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED);
    }
    return newSession(user);
  }

  @Transactional
  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }
    tokens
        .findByTokenHashAndType(TokenHasher.hash(refreshToken), AuthToken.Type.REFRESH)
        .filter(t -> t.getUsedAt() == null)
        .ifPresent(t -> t.markUsed(clock.instant()));
  }

  /** Thu hồi mọi refresh token (khi ngừng kích hoạt / ẩn danh hóa). */
  @Transactional
  public void revokeAll(UUID userId) {
    tokens.revokeAll(userId, AuthToken.Type.REFRESH, clock.instant());
  }

  public Duration refreshTokenLifetime() {
    return Duration.ofDays(properties.refreshTokenDays());
  }

  /** Mật khẩu quá 72 byte không thể khớp; vẫn so khớp chuỗi rỗng để giữ thời gian như nhau. */
  private static String candidate(String password) {
    if (password == null || password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
      return "";
    }
    return password;
  }

  private Session newSession(UserAccount user) {
    String refresh = TokenHasher.newToken();
    tokens.save(
        new AuthToken(
            user.getId(),
            AuthToken.Type.REFRESH,
            TokenHasher.hash(refresh),
            clock.instant().plus(refreshTokenLifetime())));
    return new Session(accessTokens.issue(user), refresh, user);
  }
}
