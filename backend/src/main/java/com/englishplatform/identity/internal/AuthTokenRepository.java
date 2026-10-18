package com.englishplatform.identity.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

  Optional<AuthToken> findByTokenHashAndType(String tokenHash, AuthToken.Type type);

  /**
   * Thu hồi mọi token chưa dùng của một loại (logout toàn bộ, ngừng kích hoạt, refresh bị dùng
   * lại).
   */
  @Modifying
  @Query(
      "update AuthToken t set t.usedAt = ?3 where t.userId = ?1 and t.type = ?2 and t.usedAt is null")
  int revokeAll(UUID userId, AuthToken.Type type, Instant now);

  /** Đánh dấu đã dùng nếu chưa ai dùng; trả 1 nếu thắng, 0 nếu request khác đã dùng trước. */
  @Modifying
  @Query("update AuthToken t set t.usedAt = ?2 where t.id = ?1 and t.usedAt is null")
  int markUsedIfUnused(UUID id, Instant now);
}
