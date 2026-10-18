package com.englishplatform.identity.internal;

import com.englishplatform.shared.web.BusinessException;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đồng ý phiên bản chính sách hiện hành (FR-030). */
@Service
public class ConsentService {

  private final PolicyConsentRepository consents;
  private final PrivacyProperties privacy;
  private final Clock clock;

  ConsentService(PolicyConsentRepository consents, PrivacyProperties privacy, Clock clock) {
    this.consents = consents;
    this.privacy = privacy;
    this.clock = clock;
  }

  public String currentPolicyVersion() {
    return privacy.policyVersion();
  }

  @Transactional(readOnly = true)
  public boolean hasConsentedCurrent(UUID userId) {
    return consents.existsByUserIdAndPolicyVersion(userId, privacy.policyVersion());
  }

  @Transactional
  public void accept(UUID userId, String policyVersion) {
    if (!Objects.equals(policyVersion, privacy.policyVersion())) {
      throw BusinessException.validation(
          "policyVersion", "Phiên bản chính sách không còn hiệu lực, vui lòng tải lại trang");
    }
    if (!consents.existsByUserIdAndPolicyVersion(userId, policyVersion)) {
      consents.save(new PolicyConsent(userId, policyVersion, clock.instant()));
    }
  }
}
