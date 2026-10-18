package com.englishplatform.catalog;

import java.util.UUID;

/**
 * Public API đồng bộ của catalog (events-and-ports.md §2). Các phương thức đổi trạng thái chạy
 * trong transaction của bên gọi ({@code Propagation.MANDATORY}) để số buổi và trạng thái không bao
 * giờ lệch với nghiệp vụ của bên gọi (SC-007).
 */
public interface EnrollmentApi {

  /** Ném {@code NOT_FOUND} nếu không có ghi danh. */
  EnrollmentView get(UUID enrollmentId);

  /** Đặt lịch: reserved +1. Ném {@code NO_SESSIONS_LEFT} / {@code ENROLLMENT_NOT_ACTIVE}. */
  void reserve(UUID enrollmentId);

  /** Hủy đúng hạn / giáo viên vắng / gián đoạn / hủy hệ thống: reserved −1. */
  void release(UUID enrollmentId);

  /** Hoàn thành / học viên vắng / hủy muộn: reserved −1, consumed +1. */
  void consume(UUID enrollmentId);

  /** Gián đoạn được giáo vụ tính là đã học: consumed +1 (chỗ đã được trả trước đó). */
  void consumeReleased(UUID enrollmentId);

  /**
   * Billing gọi trong transaction ghi nhận thanh toán đầu tiên (FR-025): {@code PENDING_PAYMENT} →
   * {@code ACTIVE}, {@code expires_at = now + validity_days}, phát {@link
   * CatalogEvents.EnrollmentActivated}. Ghi danh đã kích hoạt thì không đổi gì (hạn giữ nguyên).
   * Gọi đồng bộ thay cho listener {@code PaymentRecorded} để catalog không phụ thuộc billing (tránh
   * vòng catalog ↔ billing).
   */
  void activateOnFirstPayment(UUID enrollmentId);

  /** Giáo viên có ghi danh PENDING_PAYMENT/ACTIVE/EXHAUSTED/EXPIRED với học viên (FR-003). */
  boolean isAssigned(UUID teacherId, UUID studentId);
}
