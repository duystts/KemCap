package com.englishplatform.identity.internal;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserInvited;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lời mời kích hoạt tài khoản: token INVITE hạn 72h, dùng một lần. Chấp nhận lời mời yêu cầu đồng ý
 * phiên bản chính sách hiện hành; học viên dưới mốc tuổi phải có đồng ý của người giám hộ (FR-030).
 */
@Service
public class InvitationService {

  static final Duration INVITE_TTL = Duration.ofHours(72);

  private final UserAccountRepository users;
  private final AuthTokenRepository tokens;
  private final PolicyConsentRepository consents;
  private final GuardianConsentRepository guardianConsents;
  private final PrivacyProperties privacy;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  InvitationService(
      UserAccountRepository users,
      AuthTokenRepository tokens,
      PolicyConsentRepository consents,
      GuardianConsentRepository guardianConsents,
      PrivacyProperties privacy,
      PasswordEncoder passwordEncoder,
      ApplicationEventPublisher events,
      Clock clock) {
    this.users = users;
    this.tokens = tokens;
    this.consents = consents;
    this.guardianConsents = guardianConsents;
    this.privacy = privacy;
    this.passwordEncoder = passwordEncoder;
    this.events = events;
    this.clock = clock;
  }

  /** Tạo token mời mới (vô hiệu token mời cũ) và phát {@link UserInvited}. */
  @Transactional
  public void issueInvite(UserAccount user) {
    if (user.getStatus() != UserStatus.INVITED) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    Instant now = clock.instant();
    tokens.revokeAll(user.getId(), AuthToken.Type.INVITE, now);
    String token = TokenHasher.newToken();
    AuthToken invite =
        tokens.save(
            new AuthToken(
                user.getId(),
                AuthToken.Type.INVITE,
                TokenHasher.hash(token),
                now.plus(INVITE_TTL)));
    events.publishEvent(
        new UserInvited(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            invite.getId(),
            token));
  }

  @Transactional
  public void acceptInvite(String token, String password, String acceptedPolicyVersion) {
    if (acceptedPolicyVersion == null || acceptedPolicyVersion.isBlank()) {
      throw BusinessException.validation(
          "acceptedPolicyVersion", "Bạn cần đồng ý điều khoản và chính sách dữ liệu");
    }
    if (!Objects.equals(acceptedPolicyVersion, privacy.policyVersion())) {
      throw BusinessException.validation(
          "acceptedPolicyVersion",
          "Phiên bản chính sách không còn hiệu lực, vui lòng tải lại trang");
    }
    PasswordPolicy.check("password", password);
    Instant now = clock.instant();
    AuthToken invite =
        tokens
            .findByTokenHashAndType(
                TokenHasher.hash(token == null ? "" : token), AuthToken.Type.INVITE)
            .filter(t -> t.isUsable(now))
            .orElseThrow(InvitationService::invalidInvite);
    UserAccount user =
        users
            .findById(invite.getUserId())
            .filter(u -> u.getStatus() == UserStatus.INVITED)
            .orElseThrow(InvitationService::invalidInvite);
    if (needsGuardianConsent(user, now) && !guardianConsents.existsByStudentId(user.getId())) {
      throw new BusinessException(ErrorCode.GUARDIAN_CONSENT_REQUIRED);
    }
    invite.markUsed(now);
    user.activate(passwordEncoder.encode(password));
    if (!consents.existsByUserIdAndPolicyVersion(user.getId(), privacy.policyVersion())) {
      consents.save(new PolicyConsent(user.getId(), privacy.policyVersion(), now));
    }
  }

  /**
   * Học viên dưới {@code app.privacy.guardian-consent-under-age} tuổi tại thời điểm {@code now}.
   */
  boolean needsGuardianConsent(UserAccount user, Instant now) {
    if (user.getRole() != Role.STUDENT) {
      return false;
    }
    Integer age = user.ageAt(now);
    return age != null && age < privacy.guardianConsentUnderAge();
  }

  private static BusinessException invalidInvite() {
    return BusinessException.validation("token", "Link mời không hợp lệ hoặc đã hết hạn");
  }
}
