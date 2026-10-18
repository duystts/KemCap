package com.englishplatform.catalog.internal.course;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Khóa học (ví dụ "IELTS", "Giao tiếp"); tên duy nhất (FR-005). */
@Entity
@Table(name = "course")
public class Course extends BaseEntity {

  @Column(nullable = false)
  private String name;

  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CatalogItemStatus status = CatalogItemStatus.ACTIVE;

  protected Course() {}

  public Course(String name, String description) {
    update(name, description, CatalogItemStatus.ACTIVE);
  }

  public void update(String name, String description, CatalogItemStatus status) {
    this.name = name.trim();
    this.description = description;
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public CatalogItemStatus getStatus() {
    return status;
  }
}
