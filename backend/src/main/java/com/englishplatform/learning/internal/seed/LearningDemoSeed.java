package com.englishplatform.learning.internal.seed;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.learning.internal.path.LearningPath;
import com.englishplatform.learning.internal.path.LearningPathRepository;
import com.englishplatform.learning.internal.path.PathSpec;
import com.englishplatform.learning.internal.path.PathSpec.MilestoneSpec;
import com.englishplatform.learning.internal.path.PathSpec.TopicSpec;
import com.englishplatform.learning.internal.proficiency.AssessmentRepository;
import com.englishplatform.learning.internal.proficiency.GoalRepository;
import com.englishplatform.learning.internal.proficiency.LearningGoal;
import com.englishplatform.learning.internal.proficiency.ProficiencyAssessment;
import com.englishplatform.learning.internal.proficiency.ProficiencyAssessment.Cefr;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hồ sơ học tập demo (sau seed lịch): đánh giá đầu vào, mục tiêu và lộ trình 3 chặng cho
 * student1–3; student4 có ghi danh nhưng không có mục tiêu → hiện cờ "Cần tư vấn". Chạy lại không
 * tạo trùng.
 */
@Component
@Profile("demo-seed")
@Order(40)
class LearningDemoSeed implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(LearningDemoSeed.class);

  private final AssessmentRepository assessments;
  private final GoalRepository goals;
  private final LearningPathRepository paths;
  private final EnrollmentApi enrollmentApi;
  private final UserDirectory userDirectory;
  private final Clock clock;

  LearningDemoSeed(
      AssessmentRepository assessments,
      GoalRepository goals,
      LearningPathRepository paths,
      EnrollmentApi enrollmentApi,
      UserDirectory userDirectory,
      Clock clock) {
    this.assessments = assessments;
    this.goals = goals;
    this.paths = paths;
    this.enrollmentApi = enrollmentApi;
    this.userDirectory = userDirectory;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    UserView first = user("student1@demo.local");
    if (!assessments.findByStudentIdOrderByAssessedAtDescCreatedAtDesc(first.id()).isEmpty()) {
      return;
    }
    LocalDate today = clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
    seed(
        "student1@demo.local",
        LearningGoal.Type.IELTS,
        "6.5",
        Cefr.B1,
        "Lộ trình IELTS 6.5",
        today);
    seed(
        "student2@demo.local",
        LearningGoal.Type.COMMUNICATION,
        "Phỏng vấn bằng tiếng Anh",
        Cefr.A2,
        "Giao tiếp công sở",
        today);
    seed(
        "student3@demo.local",
        LearningGoal.Type.IELTS,
        "7.0",
        Cefr.B2,
        "Lộ trình IELTS 7.0",
        today);
    log.info("Demo seed: hồ sơ học tập student1–3; student4 chưa có mục tiêu (Cần tư vấn)");
  }

  private void seed(
      String email,
      LearningGoal.Type type,
      String target,
      Cefr level,
      String pathTitle,
      LocalDate today) {
    UserView student = user(email);
    var teacher = enrollmentApi.findByStudent(student.id()).getFirst().teacherId();
    assessments.save(
        new ProficiencyAssessment(
            student.id(),
            ProficiencyAssessment.Type.PLACEMENT,
            teacher,
            today.minusDays(10),
            new ProficiencyAssessment.Skills(level, level, level, level, level),
            "Đánh giá đầu vào"));
    goals.save(new LearningGoal(student.id(), type, target, today.plusMonths(4), teacher));
    paths.save(
        new LearningPath(
            student.id(),
            teacher,
            new PathSpec(
                pathTitle,
                List.of(
                    milestone(
                        "Nền tảng",
                        today.plusWeeks(4),
                        "Thì hiện tại",
                        "Từ vựng chủ đề gia đình",
                        "Phát âm cơ bản"),
                    milestone(
                        "Mở rộng",
                        today.plusWeeks(8),
                        "Thì quá khứ",
                        "Kể chuyện",
                        "Nghe hội thoại ngắn"),
                    milestone(
                        "Tăng tốc",
                        today.plusWeeks(14),
                        "Viết đoạn văn",
                        "Thảo luận ý kiến",
                        "Đề thi thử",
                        "Sửa lỗi thường gặp")))));
  }

  private static MilestoneSpec milestone(String title, LocalDate target, String... topics) {
    return new MilestoneSpec(
        null,
        title,
        target,
        null,
        java.util.Arrays.stream(topics).map(t -> new TopicSpec(null, t, null)).toList());
  }

  private UserView user(String email) {
    return userDirectory
        .findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("Thiếu tài khoản demo " + email));
  }
}
