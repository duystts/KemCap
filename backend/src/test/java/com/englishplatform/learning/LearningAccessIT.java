package com.englishplatform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.support.ApiTestSupport;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Quyền trên hồ sơ học tập (FR-003): GV không phụ trách → 404, HV chỉ đọc; cờ "Cần tư vấn"; lịch sử
 * đánh giá/điều chỉnh lộ trình; ẩn danh hóa idempotent.
 */
class LearningAccessIT extends ApiTestSupport {

  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired ApplicationEventPublisher publisher;
  @Autowired PlatformTransactionManager txManager;
  @Autowired JdbcTemplate jdbc;

  TestUser admin;
  TestUser teacher;
  TestUser otherTeacher;
  TestUser student;
  TestUser otherStudent;

  static final String ASSESSMENT =
      "{\"type\":\"PLACEMENT\",\"listening\":\"B1\",\"speaking\":\"A2\",\"reading\":\"B1\","
          + "\"writing\":\"A2\",\"grammarVocab\":\"B1\"}";

  @BeforeEach
  void setUp() throws Exception {
    admin = user(Role.ADMIN);
    teacher = user(Role.TEACHER);
    otherTeacher = user(Role.TEACHER);
    student = user(Role.STUDENT);
    otherStudent = user(Role.STUDENT);
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(courseId, new CourseService.PackageSpec("Gói", 12, 45, 90, 0));
    enrollmentService.create(student.id(), pkg.getId(), teacher.id(), null, admin.id());
  }

  @Test
  void accessMatrix() throws Exception {
    String base = "/api/v1/students/" + student.id();
    call(post(base + "/assessments"), teacher, ASSESSMENT).andExpect(status().isCreated());
    call(post(base + "/assessments"), admin, ASSESSMENT).andExpect(status().isCreated());
    call(post(base + "/assessments"), otherTeacher, ASSESSMENT).andExpect(status().isNotFound());
    call(post(base + "/assessments"), student, ASSESSMENT).andExpect(status().isForbidden());

    for (String path : new String[] {"/assessments", "/proficiency", "/goals"}) {
      call(get(base + path), student).andExpect(status().isOk());
      call(get(base + path), teacher).andExpect(status().isOk());
      call(get(base + path), otherTeacher).andExpect(status().isNotFound());
      call(get(base + path), otherStudent).andExpect(status().isNotFound());
    }
    call(post(base + "/goals"), student, "{\"type\":\"IELTS\",\"target\":\"6.5\"}")
        .andExpect(status().isForbidden());
  }

  @Test
  void assessmentHistoryIsKeptAndInvalidCefrRejected() throws Exception {
    String base = "/api/v1/students/" + student.id();
    call(post(base + "/assessments"), teacher, ASSESSMENT).andExpect(status().isCreated());
    call(
            post(base + "/assessments"),
            teacher,
            ASSESSMENT
                .replace("PLACEMENT", "PERIODIC")
                .replace("\"speaking\":\"A2\"", "\"speaking\":\"B1\""))
        .andExpect(status().isCreated());
    call(get(base + "/proficiency"), student)
        .andExpect(jsonPath("$.history.length()").value(2))
        .andExpect(jsonPath("$.current.speaking").value("B1"))
        .andExpect(jsonPath("$.current.type").value("PERIODIC"))
        // bản đầu vào giữ nguyên nội dung (NFR-PORT-03)
        .andExpect(jsonPath("$.history[1].type").value("PLACEMENT"))
        .andExpect(jsonPath("$.history[1].speaking").value("A2"));
    call(post(base + "/assessments"), teacher, ASSESSMENT.replace("\"B1\"", "\"D1\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("listening"));
    call(post(base + "/assessments"), teacher, "{\"type\":\"PLACEMENT\",\"listening\":\"B1\"}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void needsConsultingFollowsActiveGoal() throws Exception {
    String base = "/api/v1/students/" + student.id();
    call(get(base + "/proficiency"), teacher).andExpect(jsonPath("$.needsConsulting").value(true));
    call(get("/api/v1/students?needsConsulting=true"), teacher)
        .andExpect(jsonPath("$[0].id").value(student.id().toString()))
        .andExpect(jsonPath("$[0].needsConsulting").value(true));

    String goalId =
        read(
            call(post(base + "/goals"), teacher, "{\"type\":\"IELTS\",\"target\":\"6.5\"}")
                .andExpect(status().isCreated()),
            "$.id");
    call(get(base + "/proficiency"), teacher).andExpect(jsonPath("$.needsConsulting").value(false));
    call(get("/api/v1/students?needsConsulting=true"), teacher)
        .andExpect(jsonPath("$.length()").value(0));

    call(put(base + "/goals/" + goalId), teacher, "{\"deadline\":\"2099-01-01\"}")
        .andExpect(jsonPath("$.deadline").value("2099-01-01"));
    call(put(base + "/goals/" + goalId), teacher, "{\"deadline\":\"2000-01-01\"}")
        .andExpect(status().isBadRequest());
    call(put(base + "/goals/" + goalId), teacher, "{\"status\":\"DROPPED\"}")
        .andExpect(jsonPath("$.status").value("DROPPED"))
        .andExpect(jsonPath("$.deadline").value("2099-01-01"));
    call(get(base + "/proficiency"), admin).andExpect(jsonPath("$.needsConsulting").value(true));
    call(get(base + "/goals"), student).andExpect(jsonPath("$.length()").value(1));
    // học viên chưa có ghi danh không bị gắn cờ
    call(get("/api/v1/students/" + otherStudent.id() + "/proficiency"), admin)
        .andExpect(jsonPath("$.needsConsulting").value(false));
  }

  @Test
  void pathRevisionsRequireReasonAndRecordAuthor() throws Exception {
    String created =
        call(
                post("/api/v1/students/" + student.id() + "/learning-path"),
                teacher,
                "{\"title\":\"IELTS 6.5\",\"milestones\":[{\"title\":\"Nền tảng\",\"topics\":"
                    + "[{\"title\":\"Thì hiện tại\"},{\"title\":\"Thì quá khứ\"}]}]}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.completionPercent").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String pathId = com.jayway.jsonpath.JsonPath.read(created, "$.id");
    String milestoneId = com.jayway.jsonpath.JsonPath.read(created, "$.milestones[0].id");
    String topicId = com.jayway.jsonpath.JsonPath.read(created, "$.milestones[0].topics[0].id");
    String body =
        "{\"version\":0,\"title\":\"IELTS 6.5\",\"milestones\":[{\"id\":\""
            + milestoneId
            + "\",\"title\":\"Nền tảng\",\"topics\":[{\"id\":\""
            + topicId
            + "\",\"title\":\"Thì hiện tại\"},{\"title\":\"Phát âm\"}]}]";

    call(put("/api/v1/learning-paths/" + pathId), teacher, body + "}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("reason"));
    call(put("/api/v1/learning-paths/" + pathId), otherTeacher, body + ",\"reason\":\"x\"}")
        .andExpect(status().isNotFound());
    call(put("/api/v1/learning-paths/" + pathId), teacher, body + ",\"reason\":\"HV yếu phát âm\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.milestones[0].topics[0].id").value(topicId))
        .andExpect(jsonPath("$.milestones[0].topics[1].title").value("Phát âm"));
    // R1 verify U4: sửa bằng phiên bản cũ (người khác đã sửa trước) → 409
    call(put("/api/v1/learning-paths/" + pathId), admin, body + ",\"reason\":\"Sửa song song\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    call(get("/api/v1/students/" + student.id() + "/learning-path"), teacher)
        .andExpect(jsonPath("$.version").value(1));
    call(get("/api/v1/learning-paths/" + pathId + "/revisions"), teacher)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].changedBy").value(teacher.id().toString()))
        .andExpect(jsonPath("$[0].reason").value("HV yếu phát âm"))
        .andExpect(
            jsonPath("$[0].summary")
                .value(
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Thêm chủ đề Phát âm"),
                        org.hamcrest.Matchers.containsString("Xóa chủ đề Thì quá khứ"))));
    call(get("/api/v1/students/" + student.id() + "/learning-path"), student)
        .andExpect(jsonPath("$.milestones[0].topics.length()").value(2));
    call(
            post("/api/v1/students/" + student.id() + "/learning-path"),
            teacher,
            "{\"title\":\"Khác\"}")
        .andExpect(status().isConflict());
  }

  @Test
  void anonymizationRemovesLearningDataIdempotently() throws Exception {
    String base = "/api/v1/students/" + student.id();
    call(post(base + "/assessments"), teacher, ASSESSMENT).andExpect(status().isCreated());
    call(post(base + "/goals"), teacher, "{\"type\":\"IELTS\",\"target\":\"6.5\"}");
    String pathId =
        read(
            call(
                    post(base + "/learning-path"),
                    teacher,
                    "{\"title\":\"Lộ trình\",\"milestones\":[{\"title\":\"A\",\"topics\":[{\"title\":\"B\"}]}]}")
                .andExpect(status().isCreated()),
            "$.id");
    call(
            put("/api/v1/learning-paths/" + pathId),
            teacher,
            "{\"version\":0,\"title\":\"Lộ trình mới\",\"milestones\":[],\"reason\":\"Làm lại\"}")
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from path_revision where path_id = ?",
                Long.class,
                UUID.fromString(pathId)))
        .isEqualTo(1L);
    for (int i = 0; i < 2; i++) {
      new TransactionTemplate(txManager)
          .executeWithoutResult(
              s -> publisher.publishEvent(new StudentAnonymized(student.id(), UUID.randomUUID())));
    }
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(count("proficiency_assessment")).isZero();
              assertThat(count("learning_goal")).isZero();
              assertThat(count("learning_path")).isZero();
              assertThat(
                      jdbc.queryForObject(
                          "select (select count(*) from milestone m where m.path_id = ?)"
                              + " + (select count(*) from path_revision r where r.path_id = ?)",
                          Long.class,
                          UUID.fromString(pathId),
                          UUID.fromString(pathId)))
                  .isZero();
            });
  }

  private long count(String table) {
    return jdbc.queryForObject(
        "select count(*) from " + table + " where student_id = ?", Long.class, student.id());
  }
}
