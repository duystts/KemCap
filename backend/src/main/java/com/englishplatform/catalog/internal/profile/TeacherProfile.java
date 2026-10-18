package com.englishplatform.catalog.internal.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "teacher_profile")
public class TeacherProfile extends ProfileBase {

  private String bio;

  /** Ví dụ {@code IELTS}, {@code GIAO_TIEP}, {@code THIEU_NHI}. */
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(columnDefinition = "text[]", nullable = false)
  private List<String> specialties = new ArrayList<>();

  private String certificates;

  protected TeacherProfile() {}

  public TeacherProfile(UUID userId) {
    super(userId);
  }

  public void update(String bio, List<String> specialties, String certificates) {
    this.bio = bio;
    this.specialties = specialties == null ? new ArrayList<>() : new ArrayList<>(specialties);
    this.certificates = certificates;
  }

  public String getBio() {
    return bio;
  }

  public List<String> getSpecialties() {
    return List.copyOf(specialties);
  }

  public String getCertificates() {
    return certificates;
  }
}
