package com.englishplatform.learning.internal.path;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Chủ đề trong chặng; {@code DONE} khi giáo viên đánh dấu hoàn thành qua phiếu đánh giá (U6). */
@Entity
@Table(name = "topic")
public class Topic extends BaseEntity {

  public enum Status {
    PLANNED,
    DONE
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "milestone_id", nullable = false)
  private Milestone milestone;

  @Column(nullable = false)
  private int position;

  @Column(nullable = false)
  private String title;

  private String objective;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PLANNED;

  protected Topic() {}

  Topic(Milestone milestone, int position, String title, String objective) {
    this.milestone = milestone;
    update(position, title, objective);
  }

  void update(int position, String title, String objective) {
    this.position = position;
    this.title = title;
    this.objective = objective == null || objective.isBlank() ? null : objective.trim();
  }

  void moveTo(Milestone target) {
    this.milestone = target;
  }

  public void markDone() {
    status = Status.DONE;
  }

  public Milestone getMilestone() {
    return milestone;
  }

  public int getPosition() {
    return position;
  }

  public String getTitle() {
    return title;
  }

  public String getObjective() {
    return objective;
  }

  public Status getStatus() {
    return status;
  }
}
