package com.englishplatform.identity;

import java.util.UUID;

/**
 * SPI: module khác cài để từ chối ngừng kích hoạt (ví dụ catalog chặn giáo viên còn học viên). Đảo
 * chiều phụ thuộc để identity không gọi catalog — events-and-ports.md §2.
 */
public interface UserDeactivationGuard {

  /** Ném {@code BusinessException} nếu không được ngừng kích hoạt. */
  void check(UUID userId, Role role);
}
