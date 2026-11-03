package com.englishplatform.identity.internal.privacy;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.internal.GuardianConsent;
import com.englishplatform.identity.internal.GuardianConsentRepository;
import com.englishplatform.identity.internal.InvitationService;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAccountRepository;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồng ý của người giám hộ (FR-030): lần ghi đầu đưa học viên {@code AWAITING_GUARDIAN_CONSENT} về
 * {@code INVITED} và gửi lời mời; ghi lại chỉ sửa thông tin, không mời lần nữa.
 */
@Service
public class GuardianConsentService {

  private final GuardianConsentRepository consents;
  private final UserAccountRepository users;
  private final InvitationService invitations;
  private final Clock clock;

  GuardianConsentService(
      GuardianConsentRepository consents,
      UserAccountRepository users,
      InvitationService invitations,
      Clock clock) {
    this.consents = consents;
    this.users = users;
    this.invitations = invitations;
    this.clock = clock;
  }

  public record ConsentInput(
      String guardianName, String relationship, String guardianContact, LocalDate consentedOn) {}

  @Transactional(readOnly = true)
  public GuardianConsent get(UUID studentId) {
    return consents
        .findByStudentId(studentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  public GuardianConsent save(UUID studentId, ConsentInput input, UUID recordedBy) {
    UserAccount student =
        users
            .findForUpdate(studentId)
            .filter(u -> u.getRole() == Role.STUDENT)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (student.isAnonymized()) {
      throw new BusinessException(ErrorCode.ANONYMIZED);
    }
    LocalDate today = clock.instant().atZone(ZoneId.of(UserAccount.DEFAULT_TIMEZONE)).toLocalDate();
    if (input.consentedOn().isAfter(today)) {
      throw BusinessException.validation("consentedOn", "Ngày đồng ý không được sau hôm nay");
    }
    GuardianConsent existing = consents.findByStudentId(studentId).orElse(null);
    if (existing != null) {
      existing.update(
          input.guardianName(),
          input.relationship(),
          input.guardianContact(),
          input.consentedOn(),
          recordedBy);
      return existing;
    }
    // INVITED thiếu đồng ý: hồ sơ cũ đã kẹt trước khi có chặn ở issueInvite, cần đường gỡ
    boolean awaiting =
        student.getStatus() == UserStatus.AWAITING_GUARDIAN_CONSENT
            || (student.getStatus() == UserStatus.INVITED
                && invitations.missingGuardianConsent(student));
    if (!awaiting) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    GuardianConsent saved =
        consents.save(
            new GuardianConsent(
                studentId,
                input.guardianName(),
                input.relationship(),
                input.guardianContact(),
                input.consentedOn(),
                recordedBy));
    student.setStatus(UserStatus.INVITED);
    invitations.issueInvite(student);
    return saved;
  }
}
