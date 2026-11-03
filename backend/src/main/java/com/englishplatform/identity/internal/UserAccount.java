package com.englishplatform.identity.internal;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.UserView;
import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

@Entity
@Table(name = "user_account")
public class UserAccount extends BaseEntity {

  public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

  @Column(nullable = false)
  private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false)
  private String timezone = DEFAULT_TIMEZONE;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "anonymized_at")
  private Instant anonymizedAt;

  /** Request đọc hồ sơ trước lúc ẩn danh hóa không được ghi đè định danh trở lại (FR-031). */
  @Version private long version;

  protected UserAccount() {}

  public UserAccount(
      String email, String fullName, Role role, LocalDate dateOfBirth, UserStatus status) {
    this.email = email.trim().toLowerCase();
    this.fullName = fullName.trim();
    this.role = role;
    this.dateOfBirth = role == Role.STUDENT ? dateOfBirth : null;
    this.status = status;
  }

  public UserView toView() {
    return new UserView(getId(), email, fullName, role, status, timezone);
  }

  /** Tuổi tròn tại ngày {@code now} theo múi giờ Việt Nam; rỗng nếu không có ngày sinh. */
  public Integer ageAt(Instant now) {
    if (dateOfBirth == null) {
      return null;
    }
    LocalDate today = now.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toLocalDate();
    return Period.between(dateOfBirth, today).getYears();
  }

  public boolean isAnonymized() {
    return anonymizedAt != null;
  }

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }

  public void activate(String passwordHash) {
    this.passwordHash = passwordHash;
    this.status = UserStatus.ACTIVE;
  }

  public void changePassword(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void updateProfile(String fullName, String phone, String timezone) {
    if (fullName != null && !fullName.isBlank()) {
      this.fullName = fullName.trim();
    }
    if (phone != null) {
      this.phone = phone.isBlank() ? null : phone.trim();
    }
    if (timezone != null) {
      this.timezone = timezone;
    }
  }

  /** FR-031: ghi đè định danh, ngừng kích hoạt vĩnh viễn (data-model.md). Không khôi phục được. */
  public void anonymize(Instant now) {
    this.email = "anonymized+" + getId() + "@invalid";
    this.fullName = "Học viên đã ẩn danh";
    this.phone = null;
    this.dateOfBirth = null;
    this.passwordHash = null;
    this.status = UserStatus.INACTIVE;
    this.anonymizedAt = now;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getFullName() {
    return fullName;
  }

  public String getPhone() {
    return phone;
  }

  public Role getRole() {
    return role;
  }

  public String getTimezone() {
    return timezone;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public UserStatus getStatus() {
    return status;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public Instant getAnonymizedAt() {
    return anonymizedAt;
  }
}
