package com.englishplatform.learning.internal.proficiency;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Một lần đánh giá năng lực (đầu vào hoặc định kỳ), 5 kỹ năng theo CEFR. Chỉ thêm, không sửa: hồ sơ
 * hiện tại = bản mới nhất, lịch sử = toàn bộ (FR-016, NFR-PORT-03).
 */
@Entity
@Table(name = "proficiency_assessment")
public class ProficiencyAssessment extends BaseEntity {

  public enum Type {
    PLACEMENT,
    PERIODIC
  }

  public enum Cefr {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2
  }

  /** Mức của 5 kỹ năng. */
  public record Skills(
      Cefr listening, Cefr speaking, Cefr reading, Cefr writing, Cefr grammarVocab) {}

  @Column(name = "student_id", nullable = false, updatable = false)
  private UUID studentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Type type;

  @Column(name = "assessed_by", nullable = false, updatable = false)
  private UUID assessedBy;

  @Column(name = "assessed_at", nullable = false, updatable = false)
  private LocalDate assessedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Cefr listening;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Cefr speaking;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Cefr reading;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private Cefr writing;

  @Enumerated(EnumType.STRING)
  @Column(name = "grammar_vocab", nullable = false, updatable = false)
  private Cefr grammarVocab;

  @Column(updatable = false)
  private String notes;

  protected ProficiencyAssessment() {}

  public ProficiencyAssessment(
      UUID studentId,
      Type type,
      UUID assessedBy,
      LocalDate assessedAt,
      Skills skills,
      String notes) {
    this.studentId = studentId;
    this.type = type;
    this.assessedBy = assessedBy;
    this.assessedAt = assessedAt;
    this.listening = skills.listening();
    this.speaking = skills.speaking();
    this.reading = skills.reading();
    this.writing = skills.writing();
    this.grammarVocab = skills.grammarVocab();
    this.notes = notes;
  }

  public Skills skills() {
    return new Skills(listening, speaking, reading, writing, grammarVocab);
  }

  public UUID getStudentId() {
    return studentId;
  }

  public Type getType() {
    return type;
  }

  public UUID getAssessedBy() {
    return assessedBy;
  }

  public LocalDate getAssessedAt() {
    return assessedAt;
  }

  public String getNotes() {
    return notes;
  }
}
