package com.englishplatform.shared.security;

import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Người dùng hiện tại lấy từ access token (claims {@code sub}, {@code role}, {@code tz}). */
public record CurrentUser(UUID id, String role, String name, String timezone) {

  public static final String JWT_ISSUER = "english-platform";
  public static final String CLAIM_ROLE = "role";
  public static final String CLAIM_NAME = "name";
  public static final String CLAIM_TIMEZONE = "tz";

  public static Optional<CurrentUser> find() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
      return Optional.empty();
    }
    return Optional.of(
        new CurrentUser(
            UUID.fromString(jwt.getSubject()),
            jwt.getClaimAsString(CLAIM_ROLE),
            jwt.getClaimAsString(CLAIM_NAME),
            jwt.getClaimAsString(CLAIM_TIMEZONE)));
  }

  public static CurrentUser require() {
    return find().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
  }

  public boolean hasRole(String expected) {
    return expected.equals(role);
  }
}
