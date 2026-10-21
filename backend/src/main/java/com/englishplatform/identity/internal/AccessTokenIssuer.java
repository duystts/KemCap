package com.englishplatform.identity.internal;

import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.security.SecurityProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Access token JWT HS256, hạn {@code app.security.access-token-minutes} (mặc định 15 phút). */
@Component
class AccessTokenIssuer {

  private final JwtEncoder encoder;
  private final SecurityProperties properties;
  private final Clock clock;

  AccessTokenIssuer(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  String issue(UserAccount user) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(CurrentUser.JWT_ISSUER)
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(Duration.ofMinutes(properties.accessTokenMinutes())))
            .claim(CurrentUser.CLAIM_ROLE, user.getRole().name())
            .claim(CurrentUser.CLAIM_NAME, user.getFullName())
            .claim(CurrentUser.CLAIM_TIMEZONE, user.getTimezone())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
