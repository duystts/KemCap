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

  public void recordLogin(Instant at) {
    this.lastLoginAt = at;
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
