package com.englishplatform.learning.internal.path;

import com.englishplatform.shared.persistence.BaseEntity;
import com.englishplatform.shared.web.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Lộ trình cá nhân của học viên (FR-017): chặng → chủ đề. Mỗi học viên tối đa một lộ trình {@code
 * ACTIVE} (unique index). {@link #apply} thay cả cây và trả danh sách thay đổi để ghi {@code
 * path_revision} (FR-018).
 */
@Entity
@Table(name = "learning_path")
public class LearningPath extends BaseEntity {

  static final int MAX_TITLE = 200;

  public enum Status {
    ACTIVE,
    ARCHIVED
  }

  @Column(name = "student_id", nullable = false, updatable = false)
  private UUID studentId;

  @Column(name = "created_by", nullable = false, updatable = false)
  private UUID createdBy;

  @Column(nullable = false)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.ACTIVE;

  @Version private long version;

  @OneToMany(mappedBy = "path", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position")
  private List<Milestone> milestones = new ArrayList<>();

  protected LearningPath() {}

  public LearningPath(UUID studentId, UUID createdBy, PathSpec spec) {
    this.studentId = studentId;
    this.createdBy = createdBy;
    apply(spec);
  }

  /** Kết quả {@link #apply}: các dòng mô tả thay đổi + chủ đề bị xóa (service xóa khỏi DB). */
  public record Changes(List<String> lines, List<Topic> removedTopics) {
    public boolean isEmpty() {
      return lines.isEmpty();
    }
  }

  /**
   * Thay cây theo {@code spec} — giữ id và trạng thái của chặng/chủ đề cũ (kể cả chủ đề được chuyển
   * sang chặng khác) và mô tả mọi thay đổi: thêm/xóa/đổi tên, đổi mốc, đổi trạng thái chặng, sắp
   * xếp lại, chuyển chủ đề (FR-018). Id không thuộc lộ trình này được coi là phần tử mới.
   */
  public Changes apply(PathSpec spec) {
    List<String> changes = new ArrayList<>();
    String newTitle = requireTitle(spec.title(), "title");
    if (title != null && !title.equals(newTitle)) {
      changes.add("Đổi tên lộ trình \"" + title + "\" → \"" + newTitle + "\"");
    }
    title = newTitle;
    List<PathSpec.MilestoneSpec> specs = spec.milestones() == null ? List.of() : spec.milestones();
    Map<UUID, Milestone> existing = new HashMap<>();
    milestones.forEach(m -> existing.put(m.getId(), m));
    Map<UUID, Topic> topicsById = new HashMap<>();
    milestones.forEach(m -> m.getTopics().forEach(t -> topicsById.put(t.getId(), t)));
    List<Milestone> next = new ArrayList<>();
    boolean reordered = false;
    for (int i = 0; i < specs.size(); i++) {
      PathSpec.MilestoneSpec ms = specs.get(i);
      String msTitle = requireTitle(ms.title(), "milestones[" + i + "].title");
      Milestone m = ms.id() == null ? null : existing.remove(ms.id());
      if (m == null) {
        m = new Milestone(this, i, msTitle, ms.targetDate());
        changes.add("Thêm chặng " + msTitle);
      } else {
        if (!m.getTitle().equals(msTitle)) {
          changes.add("Đổi tên chặng \"" + m.getTitle() + "\" → \"" + msTitle + "\"");
        }
        if (!Objects.equals(m.getTargetDate(), ms.targetDate())) {
          changes.add(
              "Đổi mốc chặng " + msTitle + ": " + m.getTargetDate() + " → " + ms.targetDate());
        }
        reordered |= m.getPosition() != i;
        m.update(i, msTitle, ms.targetDate());
      }
      if (ms.status() != null && ms.status() != m.getStatus()) {
        changes.add("Đổi trạng thái chặng " + msTitle + ": " + m.getStatus() + " → " + ms.status());
        m.setStatus(ms.status());
      }
      changes.addAll(m.applyTopics(ms.topics() == null ? List.of() : ms.topics(), i, topicsById));
      next.add(m);
    }
    if (reordered) {
      changes.add("Sắp xếp lại thứ tự các chặng");
    }
    existing.values().forEach(m -> changes.add("Xóa chặng " + m.getTitle()));
    // chủ đề còn lại trong map = không xuất hiện ở đâu trong cây mới
    List<Topic> removed = new ArrayList<>(topicsById.values());
    removed.forEach(
        t ->
            changes.add(
                "Xóa chủ đề " + t.getTitle() + " (chặng " + t.getMilestone().getTitle() + ")"));
    removed.forEach(t -> t.getMilestone().detach(t));
    milestones.clear();
    milestones.addAll(next);
    return new Changes(changes, removed);
  }

  public int completionPercent() {
    long total = milestones.stream().mapToLong(m -> m.getTopics().size()).sum();
    if (total == 0) {
      return 0;
    }
    long done =
        milestones.stream()
            .flatMap(m -> m.getTopics().stream())
            .filter(t -> t.getStatus() == Topic.Status.DONE)
            .count();
    return (int) Math.round(done * 100.0 / total);
  }

  static String requireTitle(String value, String field) {
    if (value == null || value.isBlank()) {
      throw BusinessException.validation(field, "Không được để trống");
    }
    if (value.trim().length() > MAX_TITLE) {
      throw BusinessException.validation(field, "Tối đa 200 ký tự");
    }
    return value.trim();
  }

  public UUID getStudentId() {
    return studentId;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public String getTitle() {
    return title;
  }

  public Status getStatus() {
    return status;
  }

  public long getVersion() {
    return version;
  }

  public List<Milestone> getMilestones() {
    return List.copyOf(milestones);
  }
}
