package com.englishplatform.learning.internal.path;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Chặng của lộ trình; {@code position} duy nhất trong lộ trình. */
@Entity
@Table(name = "milestone")
public class Milestone extends BaseEntity {

  public enum Status {
    PLANNED,
    IN_PROGRESS,
    DONE
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "path_id", nullable = false, updatable = false)
  private LearningPath path;

  @Column(nullable = false)
  private int position;

  @Column(nullable = false)
  private String title;

  @Column(name = "target_date")
  private LocalDate targetDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PLANNED;

  // không orphanRemoval: chủ đề chuyển sang chặng khác phải giữ nguyên id/trạng thái; chủ đề bị xóa
  // được service xóa tường minh
  @OneToMany(
      mappedBy = "milestone",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OrderBy("position")
  private List<Topic> topics = new ArrayList<>();

  protected Milestone() {}

  Milestone(LearningPath path, int position, String title, LocalDate targetDate) {
    this.path = path;
    update(position, title, targetDate);
  }

  void update(int position, String title, LocalDate targetDate) {
    this.position = position;
    this.title = title;
    this.targetDate = targetDate;
  }

  void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Thay danh sách chủ đề theo {@code specs}; chủ đề cũ lấy từ {@code topicsById} (của cả lộ trình,
   * phần tử được dùng bị gỡ khỏi map). Trả mô tả thay đổi.
   */
  List<String> applyTopics(
      List<PathSpec.TopicSpec> specs, int milestoneIndex, Map<UUID, Topic> topicsById) {
    List<String> changes = new ArrayList<>();
    List<Topic> next = new ArrayList<>();
    boolean reordered = false;
    for (int i = 0; i < specs.size(); i++) {
      PathSpec.TopicSpec ts = specs.get(i);
      String topicTitle =
          LearningPath.requireTitle(
              ts.title(), "milestones[" + milestoneIndex + "].topics[" + i + "].title");
      Topic t = ts.id() == null ? null : topicsById.remove(ts.id());
      if (t == null) {
        t = new Topic(this, i, topicTitle, ts.objective());
        changes.add("Thêm chủ đề " + topicTitle + " (chặng " + title + ")");
      } else {
        if (t.getMilestone() != this) {
          changes.add(
              "Chuyển chủ đề "
                  + topicTitle
                  + " từ chặng "
                  + t.getMilestone().getTitle()
                  + " sang chặng "
                  + title);
          t.getMilestone().detach(t);
          t.moveTo(this);
        } else {
          reordered |= t.getPosition() != i;
        }
        if (!t.getTitle().equals(topicTitle)) {
          changes.add("Đổi tên chủ đề \"" + t.getTitle() + "\" → \"" + topicTitle + "\"");
        }
        if (!Objects.equals(t.getObjective(), blankToNull(ts.objective()))) {
          changes.add("Sửa mục tiêu chủ đề " + topicTitle);
        }
        t.update(i, topicTitle, ts.objective());
      }
      next.add(t);
    }
    if (reordered) {
      changes.add("Sắp xếp lại chủ đề trong chặng " + title);
    }
    topics.clear();
    topics.addAll(next);
    return changes;
  }

  void detach(Topic topic) {
    topics.remove(topic);
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  public LearningPath getPath() {
    return path;
  }

  public int getPosition() {
    return position;
  }

  public String getTitle() {
    return title;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public Status getStatus() {
    return status;
  }

  public List<Topic> getTopics() {
    return List.copyOf(topics);
  }
}
