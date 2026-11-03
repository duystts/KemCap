package com.englishplatform.identity.internal;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  @Query("select u from UserAccount u where lower(u.email) = lower(?1)")
  Optional<UserAccount> findByEmailIgnoreCase(String email);

  /** Khóa dòng để ghi nhận đồng ý giám hộ / ẩn danh hóa song song không giẫm lên nhau. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserAccount u where u.id = ?1")
  Optional<UserAccount> findForUpdate(UUID id);

  /** Chỉ ghi một cột: đăng nhập song song không va {@code version} và không ghi lại cả hồ sơ. */
  @Modifying
  @Query("update UserAccount u set u.lastLoginAt = :at where u.id = :id and u.anonymizedAt is null")
  int recordLogin(@Param("id") UUID id, @Param("at") Instant at);

  List<UserAccount> findByRoleAndStatus(Role role, UserStatus status);

  boolean existsByIdAndStatus(UUID id, UserStatus status);

  /** {@code role}, {@code status}, {@code pattern} đều tùy chọn (null = bỏ qua). */
  @Query(
      """
      select u from UserAccount u
      where (:role is null or u.role = :role)
        and (:status is null or u.status = :status)
        and (:pattern is null or lower(u.fullName) like :pattern or lower(u.email) like :pattern)
      order by u.fullName
      """)
  List<UserAccount> search(
      @Param("role") Role role,
      @Param("status") UserStatus status,
      @Param("pattern") String pattern);

  /** {@code "%q%"} chữ thường, ký tự đại diện trong {@code q} được escape; null nếu q rỗng. */
  static String likePattern(String q) {
    if (q == null || q.isBlank()) {
      return null;
    }
    String escaped =
        q.trim()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
