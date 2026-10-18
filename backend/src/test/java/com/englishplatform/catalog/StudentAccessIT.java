package com.englishplatform.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.Role;
import com.englishplatform.support.ApiTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Giáo viên chỉ thấy học viên được phân công; học viên chỉ thấy chính mình (FR-003). */
class StudentAccessIT extends ApiTestSupport {

  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;

  TestUser admin;
  TestUser teacherA;
  TestUser teacherB;
  TestUser studentOfA;
  TestUser otherStudent;

  @BeforeEach
  void setUp() throws Exception {
    admin = user(Role.ADMIN);
    teacherA = user(Role.TEACHER);
    teacherB = user(Role.TEACHER);
    studentOfA = user(Role.STUDENT);
    otherStudent = user(Role.STUDENT);
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói", 12, 45, 90, 3_000_000));
    enrollmentService.create(studentOfA.id(), pkg.getId(), teacherA.id(), null, admin.id());
    enrollmentService.create(otherStudent.id(), pkg.getId(), teacherB.id(), null, admin.id());
  }

  @Test
  void teacherCannotReadUnassignedStudent() throws Exception {
    call(get("/api/v1/students/" + studentOfA.id() + "/profile"), teacherA)
        .andExpect(status().isOk());
    call(get("/api/v1/students/" + otherStudent.id() + "/profile"), teacherA)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void teacherListsOnlyAssignedStudents() throws Exception {
    call(get("/api/v1/students"), teacherA)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(studentOfA.id().toString()));
    // tham số teacherId của người khác bị bỏ qua với giáo viên
    call(get("/api/v1/students?teacherId=" + teacherB.id()), teacherA)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(studentOfA.id().toString()));
  }

  @Test
  void studentReadsOnlyOwnProfileAndCannotList() throws Exception {
    call(get("/api/v1/students/" + studentOfA.id() + "/profile"), studentOfA)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").doesNotExist());
    call(get("/api/v1/students/" + otherStudent.id() + "/profile"), studentOfA)
        .andExpect(status().isNotFound());
    call(get("/api/v1/students"), studentOfA).andExpect(status().isForbidden());
  }

  @Test
  void onlyAdminEditsStudentProfileAndSeesNotes() throws Exception {
    String body = "{\"selfReportedLevel\":\"B1\",\"occupation\":\"Kỹ sư\",\"notes\":\"Ghi chú\"}";
    call(put("/api/v1/students/" + studentOfA.id() + "/profile"), teacherA, body)
        .andExpect(status().isForbidden());
    call(put("/api/v1/students/" + studentOfA.id() + "/profile"), admin, body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value("Ghi chú"));
    call(get("/api/v1/students/" + studentOfA.id() + "/profile"), teacherA)
        .andExpect(jsonPath("$.selfReportedLevel").value("B1"))
        .andExpect(jsonPath("$.notes").doesNotExist());
  }

  @Test
  void teacherEditsOnlyOwnTeacherProfile() throws Exception {
    String body =
        "{\"bio\":\"10 năm kinh nghiệm\",\"specialties\":[\"IELTS\"],\"certificates\":\"TESOL\"}";
    call(put("/api/v1/teachers/" + teacherA.id() + "/profile"), teacherA, body)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.specialties[0]").value("IELTS"));
    call(put("/api/v1/teachers/" + teacherB.id() + "/profile"), teacherA, body)
        .andExpect(status().isNotFound());
  }

  @Test
  void deactivatingTeacherWithActiveEnrollmentIsRejected() throws Exception {
    call(patch("/api/v1/users/" + teacherA.id() + "/status"), admin, "{\"status\":\"INACTIVE\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TEACHER_HAS_STUDENTS"));
  }
}
