package com.englishplatform.identity.internal.privacy;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAccountRepository;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhật ký yêu cầu xem/xóa dữ liệu (FR-031). {@code ERASURE} chỉ hoàn tất qua {@link
 * AnonymizationService}; {@code ACCESS} hoàn tất sau khi giáo vụ đã cung cấp bản tóm tắt.
 */
@Service
public class DataSubjectRequestService {

  private final DataSubjectRequestRepository requests;
  private final UserAccountRepository users;
  private final Clock clock;

  DataSubjectRequestService(
      DataSubjectRequestRepository requests, UserAccountRepository users, Clock clock) {
    this.requests = requests;
    this.users = users;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<DataSubjectRequest> list(UUID studentId, DataSubjectRequest.Status status) {
    return requests.search(studentId, status);
  }

  @Transactional
  public DataSubjectRequest record(
      UUID studentId, String requesterName, DataSubjectRequest.Type type, LocalDate receivedOn) {
    UserAccount student =
        users
            .findById(studentId)
            .filter(u -> u.getRole() == Role.STUDENT)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (student.isAnonymized()) {
      throw new BusinessException(ErrorCode.ANONYMIZED);
    }
    LocalDate today = clock.instant().atZone(ZoneId.of(UserAccount.DEFAULT_TIMEZONE)).toLocalDate();
    if (receivedOn.isAfter(today)) {
      throw BusinessException.validation("receivedOn", "Ngày nhận không được sau hôm nay");
    }
    return requests.save(new DataSubjectRequest(studentId, requesterName, type, receivedOn));
  }

  @Transactional
  public DataSubjectRequest reject(UUID id, String note, UUID by) {
    DataSubjectRequest request = locked(id);
    if (!request.isReceived()) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    request.reject(note, by, clock.instant());
    return request;
  }

  @Transactional
  public DataSubjectRequest completeAccess(UUID id, UUID by) {
    DataSubjectRequest request = locked(id);
    if (!request.isReceived() || request.getType() != DataSubjectRequest.Type.ACCESS) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    request.complete(by, clock.instant());
    return request;
  }

  private DataSubjectRequest locked(UUID id) {
    return requests.findForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
