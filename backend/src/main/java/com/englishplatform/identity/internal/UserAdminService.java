package com.englishplatform.identity.internal;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDeactivated;
import com.englishplatform.identity.UserDeactivationGuard;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giáo vụ quản lý người dùng (FR-002, FR-004, FR-030): tạo + gửi lời mời, ngừng/kích hoạt lại, gửi
 * lại lời mời. Học viên dưới mốc tuổi được tạo ở {@code AWAITING_GUARDIAN_CONSENT}, chưa gửi mời.
 */
@Service
public class UserAdminService {

  private final UserAccountRepository users;
  private final InvitationService invitations;
  private final AuthService authService;
  private final List<UserDeactivationGuard> deactivationGuards;
  private final PrivacyProperties privacy;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  UserAdminService(
      UserAccountRepository users,
      InvitationService invitations,
      AuthService authService,
      List<UserDeactivationGuard> deactivationGuards,
      PrivacyProperties privacy,
      ApplicationEventPublisher events,
      Clock clock) {
    this.users = users;
    this.invitations = invitations;
    this.authService = authService;
    this.deactivationGuards = deactivationGuards;
    this.privacy = privacy;
    this.events = events;
    this.clock = clock;
  }

  public record NewUser(
      String email, String fullName, Role role, String phone, LocalDate dateOfBirth) {}

  @Transactional(readOnly = true)
  public List<UserAccount> list(Role role, UserStatus status, String q) {
    return users.search(role, status, UserAccountRepository.likePattern(q));
  }

  @Transactional(readOnly = true)
  public UserAccount get(UUID id) {
    return users.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  public UserAccount create(NewUser request) {
    if (request.role() == Role.STUDENT && request.dateOfBirth() == null) {
      throw BusinessException.validation("dateOfBirth", "Ngày sinh bắt buộc với học viên");
    }
    if (request.dateOfBirth() != null
        && !request
            .dateOfBirth()
            .isBefore(
                clock.instant().atZone(ZoneId.of(UserAccount.DEFAULT_TIMEZONE)).toLocalDate())) {
      throw BusinessException.validation("dateOfBirth", "Ngày sinh phải trước hôm nay");
    }
    if (users.findByEmailIgnoreCase(request.email().trim()).isPresent()) {
      throw new BusinessException(ErrorCode.EMAIL_TAKEN);
    }
    UserAccount user =
        new UserAccount(
            request.email(),
            request.fullName(),
            request.role(),
            request.dateOfBirth(),
            UserStatus.INVITED);
    user.updateProfile(null, request.phone(), null);
    if (isUnderage(user)) {
      // chưa gửi mời cho tới khi giáo vụ ghi nhận đồng ý của người giám hộ (U7)
      user.setStatus(UserStatus.AWAITING_GUARDIAN_CONSENT);
      return users.save(user);
    }
    users.save(user);
    invitations.issueInvite(user);
    return user;
  }

  /**
   * {@code INACTIVE}: hỏi mọi {@link UserDeactivationGuard}, thu hồi refresh token, phát {@link
   * UserDeactivated}. {@code ACTIVE}: kích hoạt lại (về {@code INVITED} nếu chưa từng đặt mật
   * khẩu).
   */
  @Transactional
  public UserAccount changeStatus(UUID id, UUID actorId, UserStatus target) {
    UserAccount user = get(id);
    if (user.isAnonymized()) {
      throw new BusinessException(ErrorCode.ANONYMIZED);
    }
    switch (target) {
      case INACTIVE -> {
        if (id.equals(actorId)) {
          throw BusinessException.validation("status", "Không thể tự ngừng kích hoạt chính mình");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
          return user;
        }
        deactivationGuards.forEach(guard -> guard.check(user.getId(), user.getRole()));
        user.setStatus(UserStatus.INACTIVE);
        authService.revokeAll(user.getId());
        events.publishEvent(new UserDeactivated(user.getId(), user.getRole()));
      }
      case ACTIVE -> {
        if (user.getStatus() != UserStatus.INACTIVE) {
          throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        user.setStatus(user.getPasswordHash() == null ? UserStatus.INVITED : UserStatus.ACTIVE);
      }
      default ->
          throw BusinessException.validation("status", "Chỉ chuyển được sang ACTIVE hoặc INACTIVE");
    }
    return user;
  }

  @Transactional
  public void resendInvite(UUID id) {
    UserAccount user = get(id);
    if (user.getStatus() != UserStatus.INVITED) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    invitations.issueInvite(user);
  }

  private boolean isUnderage(UserAccount user) {
    Integer age = user.ageAt(clock.instant());
    return user.getRole() == Role.STUDENT && age != null && age < privacy.guardianConsentUnderAge();
  }
}
