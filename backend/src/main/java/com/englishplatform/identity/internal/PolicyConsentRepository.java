package com.englishplatform.identity.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyConsentRepository extends JpaRepository<PolicyConsent, UUID> {

  boolean existsByUserIdAndPolicyVersion(UUID userId, String policyVersion);
}
