package com.englishplatform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishplatform.learning.internal.StudentAccessGuard;
import com.englishplatform.learning.internal.path.LearningPath;
import com.englishplatform.learning.internal.path.LearningPathRepository;
import com.englishplatform.learning.internal.path.LearningPathService;
import com.englishplatform.learning.internal.path.Milestone;
import com.englishplatform.learning.internal.path.PathRevisionRepository;
import com.englishplatform.learning.internal.path.PathSpec;
import com.englishplatform.learning.internal.path.PathSpec.MilestoneSpec;
import com.englishplatform.learning.internal.path.PathSpec.TopicSpec;
import com.englishplatform.learning.internal.path.Topic;
import com.englishplatform.learning.internal.path.TopicRepository;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Lộ trình: một lộ trình đang dùng, sửa phải có lý do, tóm tắt thay đổi đúng (FR-017, FR-018). */
class LearningPathServiceTest {

  static final LocalDate D1 = LocalDate.of(2026, 12, 1);
  static final LocalDate D2 = LocalDate.of(2026, 12, 15);

  UUID student = UUID.randomUUID();
  CurrentUser teacher = new CurrentUser(UUID.randomUUID(), "TEACHER", "GV", "Asia/Ho_Chi_Minh");
  LearningPathRepository paths = mock(LearningPathRepository.class);
  PathRevisionRepository revisions = mock(PathRevisionRepository.class);
  LearningPathService service =
      new LearningPathService(
          paths,
          revisions,
          mock(TopicRepository.class),
          mock(StudentAccessGuard.class),
          mock(EntityManager.class));

  @Test
  void studentHasAtMostOneActivePath() {
    when(paths.findByStudentIdAndStatus(student, LearningPath.Status.ACTIVE))
        .thenReturn(Optional.of(path()));
    assertThatThrownBy(() -> service.create(teacher, student, spec("Lộ trình 2")))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).code())
        .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
  }

  @Test
  void updateWithoutReasonIsRejected() {
    LearningPath p = path();
    when(paths.findById(p.getId())).thenReturn(Optional.of(p));
    assertThatThrownBy(() -> service.update(teacher, p.getId(), spec("IELTS"), " ", 0L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).errors())
                    .extracting(BusinessException.FieldError::field)
                    .containsExactly("reason"));
    verify(revisions, never()).save(any());
  }

  @Test
  void summaryListsAddedRemovedRenamedTopicsAndMovedDates() {
    LearningPath p = path();
    Milestone base = p.getMilestones().getFirst();
    Topic present = base.getTopics().get(0);
    Topic past = base.getTopics().get(1);
    List<String> changes =
        p.apply(
                new PathSpec(
                    "IELTS",
                    List.of(
                        new MilestoneSpec(
                            base.getId(),
                            "Nền tảng",
                            D2,
                            null,
                            List.of(
                                new TopicSpec(present.getId(), "Thì hiện tại đơn", null),
                                new TopicSpec(null, "Phát âm", null))),
                        new MilestoneSpec(null, "Tăng tốc", D2, null, List.of()))))
            .lines();
    assertThat(changes)
        .contains(
            "Đổi mốc chặng Nền tảng: " + D1 + " → " + D2,
            "Đổi tên chủ đề \"Thì hiện tại\" → \"Thì hiện tại đơn\"",
            "Thêm chủ đề Phát âm (chặng Nền tảng)",
            "Xóa chủ đề " + past.getTitle() + " (chặng Nền tảng)",
            "Thêm chặng Tăng tốc");
    // chủ đề giữ nguyên id và trạng thái
    assertThat(p.getMilestones().getFirst().getTopics().getFirst().getId())
        .isEqualTo(present.getId());
    assertThat(p.apply(sameAs(p)).isEmpty()).isTrue();
  }

  /** R2 verify U4: đổi thứ tự / trạng thái chặng cũng là điều chỉnh, phải có trong lịch sử. */
  @Test
  void reorderAndStatusChangeAreRecorded() {
    LearningPath p = twoMilestones();
    Milestone a = p.getMilestones().get(0);
    Milestone b = p.getMilestones().get(1);
    Topic t1 = a.getTopics().get(0);
    Topic t2 = a.getTopics().get(1);
    List<String> changes =
        p.apply(
                new PathSpec(
                    "IELTS",
                    List.of(
                        new MilestoneSpec(
                            b.getId(), b.getTitle(), null, Milestone.Status.IN_PROGRESS, List.of()),
                        new MilestoneSpec(
                            a.getId(),
                            a.getTitle(),
                            D1,
                            null,
                            List.of(
                                new TopicSpec(t2.getId(), t2.getTitle(), null),
                                new TopicSpec(t1.getId(), t1.getTitle(), null))))))
            .lines();
    assertThat(changes)
        .contains(
            "Sắp xếp lại thứ tự các chặng",
            "Sắp xếp lại chủ đề trong chặng Nền tảng",
            "Đổi trạng thái chặng Mở rộng: PLANNED → IN_PROGRESS");
  }

  /** R4 verify U4: chuyển chủ đề sang chặng khác giữ id và trạng thái DONE. */
  @Test
  void movingTopicKeepsIdentityAndStatus() {
    LearningPath p = twoMilestones();
    Milestone a = p.getMilestones().get(0);
    Milestone b = p.getMilestones().get(1);
    Topic moved = a.getTopics().get(0);
    moved.markDone();
    var result =
        p.apply(
            new PathSpec(
                "IELTS",
                List.of(
                    new MilestoneSpec(
                        a.getId(),
                        a.getTitle(),
                        D1,
                        null,
                        List.of(new TopicSpec(a.getTopics().get(1).getId(), "Thì quá khứ", null))),
                    new MilestoneSpec(
                        b.getId(),
                        b.getTitle(),
                        null,
                        null,
                        List.of(new TopicSpec(moved.getId(), moved.getTitle(), null))))));
    assertThat(result.lines())
        .contains("Chuyển chủ đề Thì hiện tại từ chặng Nền tảng sang chặng Mở rộng");
    assertThat(result.removedTopics()).isEmpty();
    Topic inB = p.getMilestones().get(1).getTopics().getFirst();
    assertThat(inB.getId()).isEqualTo(moved.getId());
    assertThat(inB.getStatus()).isEqualTo(Topic.Status.DONE);
    assertThat(inB.getMilestone()).isSameAs(b);
    assertThat(p.getMilestones().get(0).getTopics()).hasSize(1);
  }

  /** R7 verify U4: id của lộ trình khác không "chiếm" được phần tử của lộ trình này. */
  @Test
  void foreignIdsAreTreatedAsNewElements() {
    LearningPath p = path();
    UUID foreign = UUID.randomUUID();
    var result =
        p.apply(
            new PathSpec(
                "IELTS",
                List.of(
                    new MilestoneSpec(
                        foreign, "Lạ", null, null, List.of(new TopicSpec(foreign, "X", null))))));
    assertThat(p.getMilestones().getFirst().getId()).isNotEqualTo(foreign);
    assertThat(p.getMilestones().getFirst().getTopics().getFirst().getId()).isNotEqualTo(foreign);
    assertThat(result.removedTopics()).hasSize(2);
  }

  private LearningPath twoMilestones() {
    return new LearningPath(
        student,
        teacher.id(),
        new PathSpec(
            "IELTS",
            List.of(
                new MilestoneSpec(
                    null,
                    "Nền tảng",
                    D1,
                    null,
                    List.of(
                        new TopicSpec(null, "Thì hiện tại", null),
                        new TopicSpec(null, "Thì quá khứ", null))),
                new MilestoneSpec(null, "Mở rộng", null, null, List.of()))));
  }

  @Test
  void completionIsDoneTopicsOverAllTopics() {
    LearningPath p = path();
    assertThat(p.completionPercent()).isZero();
    p.getMilestones().getFirst().getTopics().getFirst().markDone();
    assertThat(p.completionPercent()).isEqualTo(50);
  }

  private LearningPath path() {
    return new LearningPath(
        student,
        teacher.id(),
        new PathSpec(
            "IELTS",
            List.of(
                new MilestoneSpec(
                    null,
                    "Nền tảng",
                    D1,
                    null,
                    List.of(
                        new TopicSpec(null, "Thì hiện tại", null),
                        new TopicSpec(null, "Thì quá khứ", null))))));
  }

  private static PathSpec spec(String title) {
    return new PathSpec(title, List.of());
  }

  private static PathSpec sameAs(LearningPath p) {
    return new PathSpec(
        p.getTitle(),
        p.getMilestones().stream()
            .map(
                m ->
                    new MilestoneSpec(
                        m.getId(),
                        m.getTitle(),
                        m.getTargetDate(),
                        null,
                        m.getTopics().stream()
                            .map(t -> new TopicSpec(t.getId(), t.getTitle(), t.getObjective()))
                            .toList()))
            .toList());
  }
}
