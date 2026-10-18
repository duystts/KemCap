package com.englishplatform.catalog;

import java.util.UUID;

/**
 * Public API đồng bộ của catalog (events-and-ports.md §2). Các phương thức đổi bộ đếm chạy trong
 * transaction của bên gọi để số buổi không bao giờ lệch (SC-007). Bản thật ở U2; trước đó có stub.
 */
public interface EnrollmentApi {

  EnrollmentView get(UUID enrollmentId);

  /** Đặt lịch: reserved +1. Ném {@code NO_SESSIONS_LEFT} / {@code ENROLLMENT_NOT_ACTIVE}. */
  void reserve(UUID enrollmentId);

  /** Hủy đúng hạn / giáo viên vắng / gián đoạn / hủy hệ thống: reserved −1. */
  void release(UUID enrollmentId);

  /** Hoàn thành / học viên vắng / hủy muộn: reserved −1, consumed +1. */
  void consume(UUID enrollmentId);

  /** Gián đoạn được giáo vụ tính là đã học: consumed +1 (chỗ đã được trả trước đó). */
  void consumeReleased(UUID enrollmentId);

  /** Giáo viên có ghi danh PENDING_PAYMENT/ACTIVE/EXHAUSTED/EXPIRED với học viên (FR-003). */
  boolean isAssigned(UUID teacherId, UUID studentId);
}
