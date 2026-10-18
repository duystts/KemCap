package com.englishplatform.identity.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.privacy.*}: phiên bản chính sách hiện hành và mốc tuổi cần đồng ý người giám hộ. */
@ConfigurationProperties("app.privacy")
public record PrivacyProperties(String policyVersion, int guardianConsentUnderAge) {

  public PrivacyProperties {
    if (policyVersion == null || policyVersion.isBlank()) {
      throw new IllegalStateException("app.privacy.policy-version là bắt buộc");
    }
    if (guardianConsentUnderAge <= 0) {
      guardianConsentUnderAge = 16;
    }
  }
}
