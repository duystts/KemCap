package com.englishplatform.scheduling.internal.seed;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.scheduling.SessionStatus;
import com.englishplatform.scheduling.internal.availability.AvailabilityRule;
import com.englishplatform.scheduling.internal.availability.AvailabilityRuleRepository;
import com.englishplatform.scheduling.internal.session.Session;
import com.englishplatform.scheduling.internal.session.SessionRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lịch demo (chạy sau seed catalog): teacher1, teacher2 rảnh T2–T6 18:00–21:00 và T7 08:00–11:00;
 * mỗi học viên 1–4 có 2 buổi đã học (COMPLETED) và 1 buổi sắp tới (BOOKED); student1 thêm 1 buổi
 * hôm qua chưa chốt kết quả. Bộ đếm ghi danh được đổi qua {@link EnrollmentApi} nên luôn khớp với
 * số buổi. Chạy lại không tạo trùng.
 */
@Component
@Profile("demo-seed")
@Order(30)
class SchedulingDemoSeed implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SchedulingDemoSeed.class);
  private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

  private final AvailabilityRuleRepository rules;
  private final SessionRepository sessions;
  private final EnrollmentApi enrollmentApi;
  private final UserDirectory userDirectory;
  private final Clock clock;

  SchedulingDemoSeed(
      AvailabilityRuleRepository rules,
      SessionRepository sessions,
      EnrollmentApi enrollmentApi,
      UserDirectory userDirectory,
      Clock clock) {
    this.rules = rules;
    this.sessions = sessions;
    this.enrollmentApi = enrollmentApi;
    this.userDirectory = userDirectory;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    UserView teacher1 = user("teacher1@demo.local");
    UserView teacher2 = user("teacher2@demo.local");
    if (!rules.findByTeacherIdOrderByDayOfWeekAscStartTimeAsc(teacher1.id()).isEmpty()) {
      return;
    }
    for (UserView teacher : List.of(teacher1, teacher2)) {
      List<AvailabilityRule> week = new ArrayList<>();
      for (DayOfWeek d :
          List.of(
              DayOfWeek.MONDAY,
              DayOfWeek.TUESDAY,
              DayOfWeek.WEDNESDAY,
              DayOfWeek.THURSDAY,
              DayOfWeek.FRIDAY)) {
        week.add(new AvailabilityRule(teacher.id(), d, LocalTime.of(18, 0), LocalTime.of(21, 0)));
      }
      week.add(
          new AvailabilityRule(
              teacher.id(), DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(11, 0)));
      rules.saveAll(week);
    }
    LocalDate today = clock.instant().atZone(VN).toLocalDate();
    // giờ học riêng cho từng học viên để không trùng giáo viên/học viên
    String[] students = {"student1", "student2", "student3", "student4"};
    int[] hours = {18, 19, 18, 19};
    int created = 0;
    for (int i = 0; i < students.length; i++) {
      UserView student = user(students[i] + "@demo.local");
      UUID enrollmentId = activeEnrollmentOf(student.id());
      EnrollmentView enrollment = enrollmentApi.get(enrollmentId);
      LocalTime at = LocalTime.of(hours[i], 0);
      for (int daysAgo : new int[] {7, 3}) {
        Session past =
            session(enrollment, today.minusDays(daysAgo).atTime(at).atZone(VN).toInstant());
        enrollmentApi.reserve(enrollmentId);
        past.recordOutcome(SessionStatus.COMPLETED, enrollment.teacherId(), past.getEndsAt());
        enrollmentApi.consume(enrollmentId);
        created++;
      }
      session(enrollment, today.plusDays(2).atTime(at).atZone(VN).toInstant());
      enrollmentApi.reserve(enrollmentId);
      created++;
      if (i == 0) {
        // buổi hôm qua chưa chốt kết quả → hiện ở "buổi chưa chốt" (FR-014)
        session(enrollment, today.minusDays(1).atTime(at).atZone(VN).toInstant());
        enrollmentApi.reserve(enrollmentId);
        created++;
      }
    }
    log.info("Demo seed: lịch rảnh 2 giáo viên, {} buổi học", created);
  }

  private Session session(EnrollmentView e, Instant startsAt) {
    return sessions.save(
        new Session(
            e.id(),
            e.teacherId(),
            e.studentId(),
            startsAt,
            e.sessionMinutes(),
            e.studentId(),
            null));
  }

  private UUID activeEnrollmentOf(UUID studentId) {
    return enrollmentApi.findByStudent(studentId).stream()
        .filter(e -> e.status() == EnrollmentStatus.ACTIVE)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Thiếu ghi danh demo"))
        .id();
  }

  private UserView user(String email) {
    return userDirectory
        .findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("Thiếu tài khoản demo " + email));
  }
}
