package com.englishplatform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.Role;
import com.englishplatform.scheduling.internal.availability.AvailabilityRule;
import com.englishplatform.scheduling.internal.availability.AvailabilityRuleRepository;
import com.englishplatform.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Một cặp giáo viên–học viên có ghi danh ACTIVE, để dựng buổi đã hoàn thành qua API thật. */
abstract class LearningFlowSupport extends ApiTestSupport {

  static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

  static final String SCORES = "\"scores\":{\"listening\":3,\"speaking\":4}";

  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired EnrollmentApi enrollmentApi;
  @Autowired AvailabilityRuleRepository rules;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager txManager;

  record Setup(TestUser admin, TestUser teacher, TestUser student, UUID enrollmentId) {}

  Setup setup() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    for (DayOfWeek d : DayOfWeek.values()) {
      rules.save(new AvailabilityRule(teacher.id(), d, LocalTime.of(6, 0), LocalTime.of(22, 0)));
    }
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói", 12, 45, 90, 1_000_000));
    UUID id =
        enrollmentService.create(student.id(), pkg.getId(), teacher.id(), null, admin.id()).getId();
    new TransactionTemplate(txManager)
        .executeWithoutResult(s -> enrollmentApi.activateOnFirstPayment(id));
    return new Setup(admin, teacher, student, id);
  }

  /** {@code days} ngày nữa tính từ clock hiện tại, {@code hour} giờ VN. */
  Instant at(int days, int hour) {
    return LocalDate.ofInstant(clock.instant(), VN)
        .plusDays(days)
        .atTime(hour, 0)
        .atZone(VN)
        .toInstant();
  }

  UUID book(Setup s, Instant startsAt) throws Exception {
    return UUID.fromString(
        read(
            call(
                    post("/api/v1/sessions"),
                    s.student(),
                    "{\"enrollmentId\":\""
                        + s.enrollmentId()
                        + "\",\"startsAt\":\""
                        + startsAt
                        + "\"}")
                .andExpect(status().isCreated()),
            "$.id"));
  }

  /** Đặt, tua clock qua giờ kết thúc, giáo viên chốt {@code outcome}. */
  UUID finishedSession(Setup s, int daysAhead, String outcome) throws Exception {
    Instant startsAt = at(daysAhead, 10);
    UUID id = book(s, startsAt);
    clock.set(startsAt.plus(Duration.ofMinutes(50)));
    call(
            post("/api/v1/sessions/" + id + "/outcome"),
            s.teacher(),
            "{\"outcome\":\"" + outcome + "\"}")
        .andExpect(status().isOk());
    return id;
  }

  UUID completedWithPendingReport(Setup s, int daysAhead) throws Exception {
    UUID id = finishedSession(s, daysAhead, "COMPLETED");
    awaitReportCount(id, 1);
    return id;
  }

  void awaitReportCount(UUID sessionId, long count) {
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "select count(*) from session_report where session_id = ?",
                            Long.class,
                            sessionId))
                    .isEqualTo(count));
  }

  ResultActions submitReport(TestUser as, UUID sessionId, String fields) throws Exception {
    return call(put("/api/v1/sessions/" + sessionId + "/report"), as, "{" + fields + "}");
  }

  /** Lộ trình một chặng hai chủ đề; trả id các chủ đề. */
  List<String> path(Setup s) throws Exception {
    call(
            post("/api/v1/students/" + s.student().id() + "/learning-path"),
            s.teacher(),
            "{\"title\":\"Lộ trình\",\"milestones\":[{\"title\":\"Chặng 1\",\"topics\":"
                + "[{\"title\":\"Chủ đề 1\"},{\"title\":\"Chủ đề 2\"}]}]}")
        .andExpect(status().isCreated());
    String body =
        call(get("/api/v1/students/" + s.student().id() + "/learning-path"), s.teacher())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.milestones[0].topics[*].id");
  }
}
