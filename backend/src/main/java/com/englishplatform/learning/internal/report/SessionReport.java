package com.englishplatform.learning.internal.report;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Phiếu đánh giá một buổi hoàn thành (FR-019): {@code PENDING} khi buổi kết thúc, {@code SUBMITTED}
 * khi giáo viên nộp. Ẩn danh hóa xóa điểm, nhận xét, chủ đề; giữ các mốc thời gian (FR-031).
 */
@Entity
@Table(name = "session_report")
public class SessionReport extends BaseEntity {

  public enum Status {
    PENDING,
    SUBMITTED
  }

  /** Điểm kỹ năng trong buổi, thang 1–5; kỹ năng không đánh giá là null. */
  public record Scores(
      Integer listening, Integer speaking, Integer reading, Integer writing, Integer grammarVocab) {

    boolean isEmpty() {
      return values().stream().allMatch(v -> v == null);
    }

    boolean inRange() {
      return values().stream().allMatch(v -> v == null || (v >= 1 && v <= 5));
    }

    private List<Integer> values() {
      return java.util.Arrays.asList(listening, speaking, reading, writing, grammarVocab);
    }
  }

  @Column(name = "session_id", nullable = false, updatable = false)
  private UUID sessionId;

  @Column(name = "teacher_id", nullable = false, updatable = false)
  private UUID teacherId;

  @Column(name = "student_id", nullable = false, updatable = false)
  private UUID studentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PENDING;

  @Column(name = "session_ends_at", nullable = false, updatable = false)
  private Instant sessionEndsAt;

  @Column(name = "due_at", nullable = false, updatable = false)
  private Instant dueAt;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "taught_topic_ids", columnDefinition = "uuid[]")
  private UUID[] taughtTopicIds;

  @Column(name = "score_listening")
  private Short scoreListening;

  @Column(name = "score_speaking")
  private Short scoreSpeaking;

  @Column(name = "score_reading")
  private Short scoreReading;

  @Column(name = "score_writing")
  private Short scoreWriting;

  @Column(name = "score_grammar_vocab")
  private Short scoreGrammarVocab;

  private String comment;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "overdue_notified_at")
  private Instant overdueNotifiedAt;

  @Column(name = "session_voided_at")
  private Instant sessionVoidedAt;

  @Version private long version;

  protected SessionReport() {}

  public SessionReport(
      UUID sessionId, UUID teacherId, UUID studentId, Instant sessionEndsAt, Instant dueAt) {
    this.sessionId = sessionId;
    this.teacherId = teacherId;
    this.studentId = studentId;
    this.sessionEndsAt = sessionEndsAt;
    this.dueAt = dueAt;
  }

  void submit(Scores scores, List<UUID> taughtTopics, String comment, Instant now) {
    this.status = Status.SUBMITTED;
    this.scoreListening = toShort(scores.listening());
    this.scoreSpeaking = toShort(scores.speaking());
    this.scoreReading = toShort(scores.reading());
    this.scoreWriting = toShort(scores.writing());
    this.scoreGrammarVocab = toShort(scores.grammarVocab());
    this.taughtTopicIds = taughtTopics.toArray(UUID[]::new);
    this.comment = comment;
    this.submittedAt = now;
  }

  /** Giáo vụ sửa buổi thành không hoàn thành: giữ phiếu đã nộp, không tính vào tiến bộ. */
  void markSessionVoided(Instant now) {
    sessionVoidedAt = now;
  }

  void clearSessionVoided() {
    sessionVoidedAt = null;
  }

  private static Short toShort(Integer value) {
    return value == null ? null : value.shortValue();
  }

  private static Integer toInt(Short value) {
    return value == null ? null : value.intValue();
  }

  public boolean isOverdue(Instant now) {
    return status == Status.PENDING && !now.isBefore(dueAt);
  }

  /** Null khi chưa nộp. */
  public Boolean isOnTime() {
    return submittedAt == null ? null : !submittedAt.isAfter(dueAt);
  }

  public Scores getScores() {
    return new Scores(
        toInt(scoreListening),
        toInt(scoreSpeaking),
        toInt(scoreReading),
        toInt(scoreWriting),
        toInt(scoreGrammarVocab));
  }

  public List<UUID> getTaughtTopicIds() {
    return taughtTopicIds == null ? List.of() : List.of(taughtTopicIds);
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public UUID getTeacherId() {
    return teacherId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getSessionEndsAt() {
    return sessionEndsAt;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public String getComment() {
    return comment;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }
}
