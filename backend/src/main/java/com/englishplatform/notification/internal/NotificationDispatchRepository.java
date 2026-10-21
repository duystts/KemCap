package com.englishplatform.notification.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, UUID> {

  boolean existsByDedupeKey(String dedupeKey);

  /**
   * FR-031: che địa chỉ, tiêu đề, nội dung và lỗi (thường chứa email) của mọi email gửi cho học
   * viên; bản PENDING thôi gửi. Cập nhật hàng loạt + tăng version để job gửi lại đang chạy song
   * song không ghi đè (review U5 R2–R4).
   */
  @Modifying
  @Query(
      value =
          "UPDATE notification_dispatch SET recipient = 'anonymized', subject = :scrubbed,"
              + " body_html = :scrubbed, error = NULL, next_attempt_at = NULL,"
              + " status = CASE WHEN status = 'PENDING' THEN 'FAILED' ELSE status END,"
              + " version = version + 1, updated_at = :now"
              + " WHERE user_id = :userId",
      nativeQuery = true)
  int anonymizeForUser(
      @Param("userId") UUID userId, @Param("scrubbed") String scrubbed, @Param("now") Instant now);

  /**
   * Ghi bản PENDING nếu {@code dedupe_key} chưa có; trả 0 nếu đã có. Hai transaction cùng khóa:
   * Postgres bắt bên sau chờ bên trước kết thúc rồi bỏ qua — chỉ một bên được gửi (NFR-REL-05).
   */
  @Modifying
  @Query(
      value =
          "INSERT INTO notification_dispatch (id, dedupe_key, channel, recipient, user_id, subject,"
              + " body_html, status, attempts, next_attempt_at, created_at, updated_at)"
              + " VALUES (:id, :dedupeKey, 'EMAIL', :recipient, :userId, :subject, :bodyHtml,"
              + " 'PENDING', 0, :now, :now, :now)"
              + " ON CONFLICT (dedupe_key) DO NOTHING",
      nativeQuery = true)
  int insertIfAbsent(
      @Param("id") UUID id,
      @Param("dedupeKey") String dedupeKey,
      @Param("recipient") String recipient,
      @Param("userId") UUID userId,
      @Param("subject") String subject,
      @Param("bodyHtml") String bodyHtml,
      @Param("now") Instant now);

  Optional<NotificationDispatch> findByDedupeKey(String dedupeKey);

  List<NotificationDispatch> findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
      NotificationDispatch.Status status, Instant now, Limit limit);
}
