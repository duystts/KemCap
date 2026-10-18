package com.englishplatform.identity.internal;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  @Query("select u from UserAccount u where lower(u.email) = lower(?1)")
  Optional<UserAccount> findByEmailIgnoreCase(String email);

  List<UserAccount> findByRoleAndStatus(Role role, UserStatus status);

  boolean existsByIdAndStatus(UUID id, UserStatus status);
}
