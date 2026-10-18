package com.englishplatform.shared.security;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình JWT. App từ chối khởi động khi secret ngắn hơn 32 byte (256 bit) — NFR-SEC-05. */
@ConfigurationProperties("app.security")
public record SecurityProperties(String jwtSecret, int accessTokenMinutes, int refreshTokenDays) {

  public static final int MIN_SECRET_BYTES = 32;

  public SecurityProperties {
    if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "app.security.jwt-secret phải dài ít nhất " + MIN_SECRET_BYTES + " byte (256 bit)");
    }
    if (accessTokenMinutes <= 0) {
      accessTokenMinutes = 15;
    }
    if (refreshTokenDays <= 0) {
      refreshTokenDays = 7;
    }
  }

  public byte[] secretBytes() {
    return jwtSecret.getBytes(StandardCharsets.UTF_8);
  }
}
