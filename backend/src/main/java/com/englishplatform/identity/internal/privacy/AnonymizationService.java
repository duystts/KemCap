package com.englishplatform.identity.internal.privacy;

import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.identity.internal.AuthToken;
import com.englishplatform.identity.internal.AuthTokenRepository;
import com.englishplatform.identity.internal.GuardianConsentRepository;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAccountRepository;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ẩn danh hóa học viên theo yêu cầu xóa hợp lệ (FR-031, R16): định danh trong identity đổi ngay
 * trong một transaction; các module khác tự dọn khi nhận {@link StudentAnonymized}. Số liệu tài
 * chính và số buổi được giữ nguyên (SC-007).
 */
@Service
public class AnonymizationService {

  private static final Logger log = LoggerFactory.getLogger(AnonymizationService.class);

  private final DataSubjectRequestRepository requests;
  private final UserAccountRepository users;
  private final GuardianConsentRepository guardianConsents;
  private final AuthTokenRepository tokens;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  AnonymizationService(
      DataSubjectRequestRepository requests,
      UserAccountRepository users,
      GuardianConsentRepository guardianConsents,
      AuthTokenRepository tokens,
      ApplicationEventPublisher events,
      Clock clock) {
    this.requests = requests;
    this.users = users;
    this.guardianConsents = guardianConsents;
    this.tokens = tokens;
    this.events = events;
    this.clock = clock;
  }

  @Transactional
  public DataSubjectRequest anonymize(UUID requestId, UUID by) {
    DataSubjectRequest request =
        requests
            .findForUpdate(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    UserAccount student =
        users
            .findForUpdate(request.getStudentId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (student.isAnonymized()) {
      throw new BusinessException(ErrorCode.ANONYMIZED);
    }
    if (!request.isReceived() || request.getType() != DataSubjectRequest.Type.ERASURE) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    Instant now = clock.instant();
    student.anonymize(now);
    guardianConsents.deleteByStudentId(student.getId());
    for (AuthToken.Type type : AuthToken.Type.values()) {
      tokens.revokeAll(student.getId(), type, now);
    }
    request.complete(by, now);
    events.publishEvent(new StudentAnonymized(student.getId(), request.getId()));
    // chỉ UUID, không PII: runbook dùng dòng này áp dụng lại sau khi khôi phục sao lưu (R10)
    log.info("Đã ẩn danh hóa học viên {} theo yêu cầu {}", student.getId(), request.getId());
    return request;
  }
}
