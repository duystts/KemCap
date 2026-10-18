package com.englishplatform.catalog.internal.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Hồ sơ học viên do giáo vụ nhập. Ngày sinh ở identity; trình độ chính thức ở learning (FR-016).
 */
@Entity
@Table(name = "student_profile")
public class StudentProfile extends ProfileBase {

  /** Trình độ sơ bộ giáo vụ nhập lúc tạo hồ sơ. */
  public enum CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "self_reported_level")
  private CefrLevel selfReportedLevel;

  private String occupation;

  private String notes;

  protected StudentProfile() {}

  public StudentProfile(UUID userId) {
    super(userId);
  }

  public void update(CefrLevel selfReportedLevel, String occupation, String notes) {
    this.selfReportedLevel = selfReportedLevel;
    this.occupation = occupation;
    this.notes = notes;
  }

  /** Ẩn danh hóa (FR-031): xóa dữ liệu định danh; gọi lại không đổi gì. */
  public void anonymize() {
    update(null, null, null);
  }

  public CefrLevel getSelfReportedLevel() {
    return selfReportedLevel;
  }

  public String getOccupation() {
    return occupation;
  }

  public String getNotes() {
    return notes;
  }
}
