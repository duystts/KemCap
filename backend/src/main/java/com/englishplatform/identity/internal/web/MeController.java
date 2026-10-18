package com.englishplatform.identity.internal.web;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.internal.ConsentService;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAccountRepository;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/me} — thông tin người dùng hiện tại và đồng ý chính sách (FR-030). */
@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("isAuthenticated()")
class MeController {

  private final UserAccountRepository users;
  private final ConsentService consentService;

  MeController(UserAccountRepository users, ConsentService consentService) {
    this.users = users;
    this.consentService = consentService;
  }

  record MeResponse(
      UUID id,
      String email,
      String fullName,
      String phone,
      Role role,
      UserStatus status,
      String timezone,
      boolean consentRequired,
      String currentPolicyVersion) {}

  record UpdateMeRequest(
      @Size(max = 150) String fullName, @Size(max = 20) String phone, String timezone) {}

  record ConsentRequest(@NotBlank String policyVersion) {}

  @GetMapping
  @Transactional(readOnly = true)
  MeResponse me() {
    return toResponse(current());
  }

  @PatchMapping
  @Transactional
  MeResponse update(@Valid @RequestBody UpdateMeRequest request) {
    if (request.timezone() != null) {
      try {
        ZoneId.of(request.timezone());
      } catch (DateTimeException e) {
        throw BusinessException.validation("timezone", "Múi giờ không hợp lệ");
      }
    }
    UserAccount user = current();
    user.updateProfile(request.fullName(), request.phone(), request.timezone());
    return toResponse(user);
  }

  @PostMapping("/consents")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void consent(@Valid @RequestBody ConsentRequest request) {
    consentService.accept(CurrentUser.require().id(), request.policyVersion());
  }

  private UserAccount current() {
    return users
        .findById(CurrentUser.require().id())
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
  }

  private MeResponse toResponse(UserAccount u) {
    return new MeResponse(
        u.getId(),
        u.getEmail(),
        u.getFullName(),
        u.getPhone(),
        u.getRole(),
        u.getStatus(),
        u.getTimezone(),
        !consentService.hasConsentedCurrent(u.getId()),
        consentService.currentPolicyVersion());
  }
}
