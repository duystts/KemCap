package com.englishplatform.identity.internal;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.UserView;
import com.englishplatform.shared.web.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class UserDirectoryImpl implements UserDirectory {

  private final UserAccountRepository users;

  UserDirectoryImpl(UserAccountRepository users) {
    this.users = users;
  }

  @Override
  public Optional<UserView> find(UUID userId) {
    return users.findById(userId).map(UserAccount::toView);
  }

  @Override
  public UserView requireActive(UUID userId, Role role) {
    return users
        .findById(userId)
        .filter(u -> u.getRole() == role && u.isActive())
        .map(UserAccount::toView)
        .orElseThrow(
            () ->
                BusinessException.validation(
                    role == Role.TEACHER ? "teacherId" : "userId",
                    "Người dùng không tồn tại, sai vai trò hoặc chưa kích hoạt"));
  }

  @Override
  public List<UserView> findAdmins() {
    return users.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE).stream()
        .map(UserAccount::toView)
        .toList();
  }
}
