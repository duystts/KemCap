package com.englishplatform.identity.internal;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Token một lần (INVITE, PASSWORD_RESET) hoặc refresh token; chỉ lưu SHA-256. */
@Entity
@Table(name = "auth_token")
public class AuthToken extends BaseEntity {

  public enum Type {
    INVITE,
    PASSWORD_RESET,
    REFRESH
  }

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Type type;

  @Column(name = "token_hash", nullable = false)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  protected AuthToken() {}

  public AuthToken(UUID userId, Type type, String tokenHash, Instant expiresAt) {
    this.userId = userId;
    this.type = type;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public boolean isUsable(Instant now) {
    return usedAt == null && now.isBefore(expiresAt);
  }

  public void markUsed(Instant now) {
    this.usedAt = now;
  }

  public UUID getUserId() {
    return userId;
  }

  public Type getType() {
    return type;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
