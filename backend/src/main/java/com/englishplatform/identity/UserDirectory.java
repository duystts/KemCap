package com.englishplatform.identity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Public API của identity cho mọi module. */
public interface UserDirectory {

  Optional<UserView> find(UUID userId);

  /** Ném {@code VALIDATION_FAILED} nếu người dùng không tồn tại, sai vai trò hoặc không ACTIVE. */
  UserView requireActive(UUID userId, Role role);

  List<UserView> findAdmins();

  Optional<UserView> findByEmail(String email);

  /** Người dùng theo id (bỏ qua id không tồn tại), để module khác ghép tên hiển thị. */
  List<UserView> findAll(Collection<UUID> userIds);

  /**
   * Tìm theo vai trò, trạng thái (tùy chọn) và {@code q} (tên hoặc email, không phân biệt hoa
   * thường; tùy chọn), sắp xếp theo tên.
   */
  List<UserView> search(Role role, UserStatus status, String q);
}
