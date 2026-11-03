package com.englishplatform.learning.internal.assignment;

import com.englishplatform.shared.persistence.BaseEntity;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Bài nộp văn bản (K2): nộp lại được tới khi đã chấm; chấm 0–10, một lần (MVP). */
@Entity
@Table(name = "submission")
public class Submission extends BaseEntity {

  @Column(name = "assignment_id", nullable = false, updatable = false)
  private UUID assignmentId;

  @Column(name = "text_answer", nullable = false)
  private String textAnswer;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  private Short score;

  private String feedback;

  @Column(name = "graded_by")
  private UUID gradedBy;

  @Column(name = "graded_at")
  private Instant gradedAt;

  @Version private long version;

  protected Submission() {}

  Submission(UUID assignmentId, String textAnswer, Instant now) {
    this.assignmentId = assignmentId;
    this.textAnswer = textAnswer;
    this.submittedAt = now;
  }

  boolean isGraded() {
    return gradedAt != null;
  }

  void resubmit(String textAnswer, Instant now) {
    if (isGraded()) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Bài đã chấm, không nộp lại được");
    }
    this.textAnswer = textAnswer;
    this.submittedAt = now;
  }

  void grade(int score, String feedback, UUID by, Instant now) {
    if (isGraded()) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "Bài đã được chấm");
    }
    this.score = (short) score;
    this.feedback = feedback == null || feedback.isBlank() ? null : feedback.trim();
    this.gradedBy = by;
    this.gradedAt = now;
  }

  public UUID getAssignmentId() {
    return assignmentId;
  }

  public String getTextAnswer() {
    return textAnswer;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public Integer getScore() {
    return score == null ? null : score.intValue();
  }

  public String getFeedback() {
    return feedback;
  }

  public UUID getGradedBy() {
    return gradedBy;
  }

  public Instant getGradedAt() {
    return gradedAt;
  }
}
