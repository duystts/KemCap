package com.englishplatform.catalog.internal.seed;

import com.englishplatform.catalog.internal.course.Course;
import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseRepository;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.Enrollment;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dữ liệu catalog demo (chạy sau seed identity): 2 khóa, 2 gói, ghi danh {@code ACTIVE} cho
 * student1–4 (teacher1: 1–2, teacher2: 3–4); student5 chưa ghi danh. Ghi danh được kích hoạt trực
 * tiếp, chưa có thanh toán nên công nợ = học phí. Chạy lại không tạo trùng.
 */
@Component
@Profile("demo-seed")
@Order(20)
class CatalogDemoSeed implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(CatalogDemoSeed.class);

  private final CourseRepository courses;
  private final CourseService courseService;
  private final EnrollmentService enrollmentService;
  private final UserDirectory userDirectory;
  private final Clock clock;

  CatalogDemoSeed(
      CourseRepository courses,
      CourseService courseService,
      EnrollmentService enrollmentService,
      UserDirectory userDirectory,
      Clock clock) {
    this.courses = courses;
    this.courseService = courseService;
    this.enrollmentService = enrollmentService;
    this.userDirectory = userDirectory;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!courses.findByNameIgnoreCase("IELTS").isEmpty()) {
      return;
    }
    Course ielts = courseService.createCourse("IELTS", "Luyện thi IELTS 1 kèm 1");
    Course talk = courseService.createCourse("Giao tiếp", "Tiếng Anh giao tiếp 1 kèm 1");
    CoursePackage ieltsPkg =
        courseService.createPackage(
            ielts.getId(),
            new CourseService.PackageSpec(
                "IELTS 24 buổi × 60 phút, 90 ngày", 24, 60, 90, 7_200_000));
    CoursePackage talkPkg =
        courseService.createPackage(
            talk.getId(),
            new CourseService.PackageSpec(
                "Giao tiếp 12 buổi × 45 phút, 90 ngày", 12, 45, 90, 3_000_000));

    UserView admin = user("admin@demo.local");
    UserView teacher1 = user("teacher1@demo.local");
    UserView teacher2 = user("teacher2@demo.local");
    enroll(user("student1@demo.local"), ieltsPkg, teacher1, admin);
    enroll(user("student2@demo.local"), talkPkg, teacher1, admin);
    enroll(user("student3@demo.local"), ieltsPkg, teacher2, admin);
    enroll(user("student4@demo.local"), talkPkg, teacher2, admin);
    log.info("Demo seed: 2 khóa, 2 gói, 4 ghi danh ACTIVE");
  }

  private void enroll(UserView student, CoursePackage pkg, UserView teacher, UserView admin) {
    Enrollment enrollment =
        enrollmentService.create(student.id(), pkg.getId(), teacher.id(), null, admin.id());
    enrollment.activate(clock.instant());
  }

  private UserView user(String email) {
    return userDirectory
        .findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("Thiếu tài khoản demo " + email));
  }
}
