package com.englishplatform.identity.internal;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Đồng ý của cha mẹ/người giám hộ cho học viên dưới 16 tuổi (FR-030). Ghi nhận ở U7. */
@Entity
@Table(name = "guardian_consent")
public class GuardianConsent extends BaseEntity {

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "guardian_name", nullable = false)
  private String guardianName;

  @Column(nullable = false)
  private String relationship;

  @Column(name = "guardian_contact", nullable = false)
  private String guardianContact;

  @Column(name = "consented_on", nullable = false)
  private LocalDate consentedOn;

  @Column(name = "recorded_by", nullable = false)
  private UUID recordedBy;

  protected GuardianConsent() {}

  public GuardianConsent(
      UUID studentId,
      String guardianName,
      String relationship,
      String guardianContact,
      LocalDate consentedOn,
      UUID recordedBy) {
    this.studentId = studentId;
    this.guardianName = guardianName;
    this.relationship = relationship;
    this.guardianContact = guardianContact;
    this.consentedOn = consentedOn;
    this.recordedBy = recordedBy;
  }

  public UUID getStudentId() {
    return studentId;
  }
}
