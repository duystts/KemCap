package com.englishplatform.learning.internal.proficiency;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.learning.internal.StudentAccessGuard;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Hồ sơ năng lực và mục tiêu học tập (FR-015, FR-016). */
@Service
public class ProficiencyService {

  static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  static final Set<EnrollmentStatus> STUDYING =
      EnumSet.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PENDING_PAYMENT);

  private final AssessmentRepository assessments;
  private final GoalRepository goals;
  private final StudentAccessGuard guard;
  private final EnrollmentApi enrollmentApi;
  private final Clock clock;

  ProficiencyService(
      AssessmentRepository assessments,
      GoalRepository goals,
      StudentAccessGuard guard,
      EnrollmentApi enrollmentApi,
      Clock clock) {
    this.assessments = assessments;
    this.goals = goals;
    this.guard = guard;
    this.enrollmentApi = enrollmentApi;
    this.clock = clock;
  }

  /** Hồ sơ hiện tại (bản mới nhất, có thể null), lịch sử, cờ "Cần tư vấn". */
  public record Profile(
      ProficiencyAssessment current,
      List<ProficiencyAssessment> history,
      boolean needsConsulting) {}

  @Transactional(readOnly = true)
  public List<ProficiencyAssessment> assessments(CurrentUser actor, UUID studentId) {
    guard.requireRead(actor, studentId);
    return assessments.findByStudentIdOrderByAssessedAtDescCreatedAtDesc(studentId);
  }

  @Transactional
  public ProficiencyAssessment assess(
      CurrentUser actor,
      UUID studentId,
      ProficiencyAssessment.Type type,
      LocalDate assessedAt,
      ProficiencyAssessment.Skills skills,
      String notes) {
    guard.requireWrite(actor, studentId);
    if (type == null) {
      throw BusinessException.validation("type", "Chọn loại đánh giá");
    }
    LocalDate date = assessedAt == null ? today() : assessedAt;
    if (date.isAfter(today())) {
      throw BusinessException.validation("assessedAt", "Ngày đánh giá không được sau hôm nay");
    }
    if (skills.listening() == null
        || skills.speaking() == null
        || skills.reading() == null
        || skills.writing() == null
        || skills.grammarVocab() == null) {
      throw BusinessException.validation("skills", "Cần đánh giá đủ 5 kỹ năng (A1–C2)");
    }
    return assessments.save(
        new ProficiencyAssessment(studentId, type, actor.id(), date, skills, blankToNull(notes)));
  }

  @Transactional(readOnly = true)
  public Profile profile(CurrentUser actor, UUID studentId) {
    List<ProficiencyAssessment> history = assessments(actor, studentId);
    return new Profile(
        history.isEmpty() ? null : history.getFirst(), history, needsConsulting(studentId));
  }

  @Transactional(readOnly = true)
  public List<LearningGoal> goals(CurrentUser actor, UUID studentId) {
    guard.requireRead(actor, studentId);
    return goals.findByStudentIdOrderByCreatedAtDesc(studentId);
  }

  @Transactional
  public LearningGoal addGoal(
      CurrentUser actor,
      UUID studentId,
      LearningGoal.Type type,
      String target,
      LocalDate deadline) {
    guard.requireWrite(actor, studentId);
    if (type == null) {
      throw BusinessException.validation("type", "Chọn loại mục tiêu");
    }
    if (target == null || target.isBlank()) {
      throw BusinessException.validation("target", "Nhập mức mục tiêu, ví dụ 6.5 hoặc B2");
    }
    if (deadline != null && deadline.isBefore(today())) {
      throw BusinessException.validation("deadline", "Thời hạn phải từ hôm nay trở đi");
    }
    return goals.save(new LearningGoal(studentId, type, target, deadline, actor.id()));
  }

  /** Đổi trạng thái/thời hạn; nội dung mục tiêu giữ nguyên để không mất lịch sử. */
  @Transactional
  public LearningGoal updateGoal(
      CurrentUser actor,
      UUID studentId,
      UUID goalId,
      LearningGoal.Status status,
      LocalDate deadline) {
    guard.requireWrite(actor, studentId);
    LearningGoal goal =
        goals
            .findById(goalId)
            .filter(g -> g.getStudentId().equals(studentId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (deadline != null && deadline.isBefore(today())) {
      throw BusinessException.validation("deadline", "Thời hạn phải từ hôm nay trở đi");
    }
    goal.update(status, deadline);
    return goal;
  }

  /** Đang có ghi danh {@code ACTIVE}/{@code PENDING_PAYMENT} mà chưa có mục tiêu {@code ACTIVE}. */
  boolean needsConsulting(UUID studentId) {
    boolean studying =
        enrollmentApi.findByStudent(studentId).stream()
            .anyMatch(e -> STUDYING.contains(e.status()));
    return studying && goals.findStudentsWithActiveGoal(List.of(studentId)).isEmpty();
  }

  private LocalDate today() {
    return clock.instant().atZone(BUSINESS_ZONE).toLocalDate();
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
