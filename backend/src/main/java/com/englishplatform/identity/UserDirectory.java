package com.englishplatform.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Public API của identity cho mọi module. */
public interface UserDirectory {

  Optional<UserView> find(UUID userId);

  /** Ném {@code VALIDATION_FAILED} nếu người dùng không tồn tại, sai vai trò hoặc không ACTIVE. */
  UserView requireActive(UUID userId, Role role);

  List<UserView> findAdmins();
}
