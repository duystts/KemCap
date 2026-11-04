package com.englishplatform.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.Role;
import com.englishplatform.support.ApiTestSupport;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

/**
 * Phần ma trận quyền không phủ được (review T140): đường dẫn của mình kèm id con của người khác,
 * tham số lọc trỏ tới người khác, CORS và giới hạn độ dài.
 */
class OwnershipHardeningIT extends ApiTestSupport {

  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;

  @Value("${app.frontend-url}")
  String frontendUrl;

  TestUser admin;
  TestUser teacher;
  TestUser otherTeacher;
  TestUser student;
  TestUser otherStudent;

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
    enrollmentService.create(otherStudent.id(), pkg.getId(), otherTeacher.id(), null, admin.id());
  }

  @Test
  void teacherDeletesOnlyOwnDayOff() throws Exception {
    String path = "/api/v1/teachers/" + otherTeacher.id() + "/availability/exceptions";
    String dayOff =
        read(
            call(post(path), otherTeacher, "{\"date\":\"" + LocalDate.now().plusDays(25) + "\"}")
                .andExpect(status().isCreated()),
            "$.id");

    call(delete("/api/v1/teachers/" + teacher.id() + "/availability/exceptions/" + dayOff), teacher)
        .andExpect(status().isNotFound());
    call(delete(path + "/" + dayOff), otherTeacher).andExpect(status().is2xxSuccessful());
    call(get(path), otherTeacher).andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void goalOfAnotherStudentIsNotReachableThroughOwnPath() throws Exception {
    String goal =
        read(
            call(
                    post("/api/v1/students/" + otherStudent.id() + "/goals"),
                    otherTeacher,
                    "{\"type\":\"IELTS\",\"target\":\"6.5\"}")
                .andExpect(status().isCreated()),
            "$.id");

    call(
            put("/api/v1/students/" + student.id() + "/goals/" + goal),
            teacher,
            "{\"status\":\"DROPPED\"}")
        .andExpect(status().isNotFound());
    call(get("/api/v1/students/" + otherStudent.id() + "/goals"), otherTeacher)
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
  }

  @Test
  void listFiltersCannotReachAnotherStudent() throws Exception {
    for (String list : new String[] {"/api/v1/sessions", "/api/v1/enrollments"}) {
      call(get(list + "?studentId=" + otherStudent.id()), student)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[?(@.studentId == '" + otherStudent.id() + "')]").isEmpty());
      call(get(list + "?studentId=" + otherStudent.id()), teacher)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[?(@.studentId == '" + otherStudent.id() + "')]").isEmpty());
    }
  }

  @Test
  void corsAllowsOnlyTheFrontendOrigin() throws Exception {
    mvc.perform(
            options("/api/v1/auth/login")
                .header("Origin", frontendUrl)
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", frontendUrl))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    mvc.perform(
            options("/api/v1/auth/login")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @Test
  void oversizedTextIsRejectedWith400() throws Exception {
    String tooLong = "x".repeat(101);
    call(
            put("/api/v1/teachers/" + teacher.id() + "/profile"),
            teacher,
            "{\"specialties\":[\"" + tooLong + "\"]}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("specialties[0]"));

    call(
            post("/api/v1/courses"),
            admin,
            "{\"name\":\"Khóa\",\"description\":\"" + "x".repeat(5001) + "\"}")
        .andExpect(status().isBadRequest());

    String path =
        "{\"title\":\"Lộ trình\",\"milestones\":[{\"title\":\"Chặng\",\"topics\":"
            + "[{\"title\":\"Chủ đề\",\"objective\":\""
            + "x".repeat(2001)
            + "\"}]}]}";
    call(post("/api/v1/students/" + student.id() + "/learning-path"), teacher, path)
        .andExpect(status().isBadRequest());

    mvc.perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + "x".repeat(255) + "\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.c\",\"password\":\"" + "x".repeat(201) + "\"}"))
        .andExpect(status().isBadRequest());
  }
}
