package com.englishplatform.identity.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianConsentRepository extends JpaRepository<GuardianConsent, UUID> {

  boolean existsByStudentId(UUID studentId);
}
