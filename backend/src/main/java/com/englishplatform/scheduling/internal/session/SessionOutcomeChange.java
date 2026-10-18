package com.englishplatform.scheduling.internal.session;

import com.englishplatform.scheduling.SessionStatus;
import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/** Một lần chốt / sửa kết quả / quyết định Gián đoạn của buổi học (chỉ thêm). */
@Entity
@Table(name = "session_outcome_change")
public class SessionOutcomeChange extends BaseEntity {

  @Column(name = "session_id", nullable = false, updatable = false)
  private UUID sessionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", nullable = false, updatable = false)
  private SessionStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, updatable = false)
  private SessionStatus toStatus;

  @Column(name = "changed_by", nullable = false, updatable = false)
  private UUID changedBy;

  @Column(updatable = false)
  private String note;

  protected SessionOutcomeChange() {}

  SessionOutcomeChange(
      UUID sessionId,
      SessionStatus fromStatus,
      SessionStatus toStatus,
      UUID changedBy,
      String note) {
    this.sessionId = sessionId;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.changedBy = changedBy;
    this.note = note == null || note.isBlank() ? null : note.trim();
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public SessionStatus getFromStatus() {
    return fromStatus;
  }

  public SessionStatus getToStatus() {
    return toStatus;
  }

  public UUID getChangedBy() {
    return changedBy;
  }

  public String getNote() {
    return note;
  }
}
