package com.englishplatform.identity.internal;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Người dùng đồng ý một phiên bản điều khoản & chính sách dữ liệu (FR-030). */
@Entity
@Table(name = "policy_consent")
public class PolicyConsent extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "policy_version", nullable = false)
  private String policyVersion;

  @Column(name = "accepted_at", nullable = false)
  private Instant acceptedAt;

  protected PolicyConsent() {}

  public PolicyConsent(UUID userId, String policyVersion, Instant acceptedAt) {
    this.userId = userId;
    this.policyVersion = policyVersion;
    this.acceptedAt = acceptedAt;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getPolicyVersion() {
    return policyVersion;
  }
}
