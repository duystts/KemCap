package com.englishplatform.identity.internal.web;

import com.englishplatform.identity.internal.GuardianConsent;
import com.englishplatform.identity.internal.privacy.GuardianConsentService;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/students/{id}/guardian-consent} — giáo vụ ghi nhận đồng ý (FR-030). */
@RestController
@RequestMapping("/api/v1/students/{studentId}/guardian-consent")
@PreAuthorize("hasRole('ADMIN')")
class GuardianConsentController {

  private final GuardianConsentService service;

  GuardianConsentController(GuardianConsentService service) {
    this.service = service;
  }

  record GuardianConsentDto(
      UUID studentId,
      String guardianName,
      String relationship,
      String guardianContact,
      LocalDate consentedOn,
      UUID recordedBy) {

    static GuardianConsentDto of(GuardianConsent c) {
      return new GuardianConsentDto(
          c.getStudentId(),
          c.getGuardianName(),
          c.getRelationship(),
          c.getGuardianContact(),
          c.getConsentedOn(),
          c.getRecordedBy());
    }
  }

  record ConsentRequest(
      @NotBlank @Size(max = 150) String guardianName,
      @NotBlank @Size(max = 50) String relationship,
      @NotBlank @Size(max = 255) String guardianContact,
      @NotNull LocalDate consentedOn) {}

  @GetMapping
  GuardianConsentDto get(@PathVariable UUID studentId) {
    return GuardianConsentDto.of(service.get(studentId));
  }

  @PutMapping
  GuardianConsentDto save(@PathVariable UUID studentId, @Valid @RequestBody ConsentRequest r) {
    return GuardianConsentDto.of(
        service.save(
            studentId,
            new GuardianConsentService.ConsentInput(
                r.guardianName().trim(),
                r.relationship().trim(),
                r.guardianContact().trim(),
                r.consentedOn()),
            CurrentUser.require().id()));
  }
}
