package com.englishplatform.notification.internal;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Một email cần gửi. {@code dedupe_key} unique chống gửi trùng; lỗi thì thử lại tối đa 3 lần sau
 * lần gửi đầu với backoff 1 → 5 → 15 phút rồi {@code FAILED} (NFR-REL-06). Nội dung (có thể chứa
 * token một lần và họ tên) bị xóa ngay khi bản ghi kết thúc ở {@code SENT}/{@code FAILED}
 * (NFR-SEC-03); chỉ bản {@code PENDING} giữ nội dung trong cửa sổ thử lại ≤ ~21 phút.
 */
@Entity
@Table(name = "notification_dispatch")
public class NotificationDispatch extends BaseEntity {

  static final String SCRUBBED_BODY = "[đã xóa sau khi xử lý]";

  static final List<Duration> BACKOFF =
      List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15));

  public enum Status {
    PENDING,
    SENT,
    FAILED
  }

  @Column(name = "dedupe_key", nullable = false)
  private String dedupeKey;

  @Column(nullable = false)
  private String channel = "EMAIL";

  @Column(nullable = false)
  private String recipient;

  @Column(nullable = false)
  private String subject;

  @Column(name = "body_html", nullable = false)
  private String bodyHtml;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PENDING;

  @Column(nullable = false)
  private short attempts;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  private String error;

  @Column(name = "sent_at")
  private Instant sentAt;

  protected NotificationDispatch() {}

  NotificationDispatch(
      String dedupeKey, String recipient, String subject, String bodyHtml, Instant now) {
    this.dedupeKey = dedupeKey;
    this.recipient = recipient;
    this.subject = subject;
    this.bodyHtml = bodyHtml;
    this.nextAttemptAt = now;
  }

  void markSent(Instant now) {
    attempts++;
    status = Status.SENT;
    sentAt = now;
    nextAttemptAt = null;
    error = null;
    bodyHtml = SCRUBBED_BODY;
  }

  void markFailed(Instant now, String message) {
    attempts++;
    error = message;
    int retriesDone = attempts - 1;
    if (retriesDone >= BACKOFF.size()) {
      status = Status.FAILED;
      nextAttemptAt = null;
      bodyHtml = SCRUBBED_BODY;
    } else {
      nextAttemptAt = now.plus(BACKOFF.get(retriesDone));
    }
  }

  public String getDedupeKey() {
    return dedupeKey;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getSubject() {
    return subject;
  }

  public String getBodyHtml() {
    return bodyHtml;
  }

  public Status getStatus() {
    return status;
  }

  public String getError() {
    return error;
  }

  public short getAttempts() {
    return attempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }
}
