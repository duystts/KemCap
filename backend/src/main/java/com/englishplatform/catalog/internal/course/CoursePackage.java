package com.englishplatform.catalog.internal.course;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Gói học (bảng {@code package}; tên lớp tránh trùng {@code java.lang.Package}). Sửa giá gói không
 * ảnh hưởng ghi danh đã tạo vì ghi danh sao chép giá lúc tạo.
 */
@Entity
@Table(name = "package")
public class CoursePackage extends BaseEntity {

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(nullable = false)
  private String name;

  @Column(name = "session_count", nullable = false)
  private int sessionCount;

  @Column(name = "session_minutes", nullable = false)
  private int sessionMinutes;

  @Column(name = "validity_days", nullable = false)
  private int validityDays;

  @Column(name = "price_vnd", nullable = false)
  private long priceVnd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CatalogItemStatus status = CatalogItemStatus.ACTIVE;

  protected CoursePackage() {}

  public CoursePackage(
      UUID courseId,
      String name,
      int sessionCount,
      int sessionMinutes,
      int validityDays,
      long priceVnd) {
    this.courseId = courseId;
    update(name, sessionCount, sessionMinutes, validityDays, priceVnd, CatalogItemStatus.ACTIVE);
  }

  public void update(
      String name,
      int sessionCount,
      int sessionMinutes,
      int validityDays,
      long priceVnd,
      CatalogItemStatus status) {
    this.name = name.trim();
    this.sessionCount = sessionCount;
    this.sessionMinutes = sessionMinutes;
    this.validityDays = validityDays;
    this.priceVnd = priceVnd;
    this.status = status;
  }

  public boolean isActive() {
    return status == CatalogItemStatus.ACTIVE;
  }

  public UUID getCourseId() {
    return courseId;
  }

  public String getName() {
    return name;
  }

  public int getSessionCount() {
    return sessionCount;
  }

  public int getSessionMinutes() {
    return sessionMinutes;
  }

  public int getValidityDays() {
    return validityDays;
  }

  public long getPriceVnd() {
    return priceVnd;
  }

  public CatalogItemStatus getStatus() {
    return status;
  }
}
