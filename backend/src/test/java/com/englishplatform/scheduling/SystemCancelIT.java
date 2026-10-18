package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.CatalogEvents.TeacherReassigned;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.scheduling.internal.session.Session;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;

/**
 * Hủy hệ thống (FR-006, FR-004, FR-031): buổi {@code BOOKED} tương lai → {@code CANCELLED_SYSTEM},
 * chỗ được trả; buổi đã qua không bị đụng; phát lại event không lỗi.
 */
@EnableScenarios
class SystemCancelIT extends SchedulingTestSupport {

  @Test
  void teacherReassignmentCancelsFutureSessionsWithOldTeacher(Scenario scenario) throws Exception {
    Setup s = setup(6);
    UUID future = bookOk(s.student(), s.enrollmentId(), at(3, 10, 0));
    UUID past = pastSession(s);
    TestUser newTeacher = user(Role.TEACHER);

    scenario
        .stimulate(
            () ->
                unchecked(
                    () ->
                        call(
                            put("/api/v1/enrollments/" + s.enrollmentId() + "/teacher"),
                            s.admin(),
                            "{\"teacherId\":\""
                                + newTeacher.id()
                                + "\",\"reason\":\"GV nghỉ dài hạn\"}")))
        .andWaitForStateChange(() -> sessionStatus(future) == SessionStatus.CANCELLED_SYSTEM)
        .andVerify(
            ok -> {
              assertThat(sessionStatus(past)).isEqualTo(SessionStatus.COMPLETED);
              assertThat(enrollment(s.enrollmentId()).sessionsReserved()).isZero();
              assertThat(enrollment(s.enrollmentId()).sessionsConsumed()).isEqualTo(1);
            });

    // phát lại event: không lỗi, không trả chỗ lần hai
    TeacherReassigned replay =
        new TeacherReassigned(
            s.enrollmentId(), s.student().id(), s.teacher().id(), newTeacher.id());
    scenario
        .publish(replay)
        .andWaitForStateChange(() -> enrollment(s.enrollmentId()))
        .andVerify(e -> assertThat(e.sessionsReserved()).isZero());
  }

  @Test
  void deactivatedStudentLosesFutureSessions(Scenario scenario) throws Exception {
    Setup s = setup(4);
    UUID future = bookOk(s.student(), s.enrollmentId(), at(2, 9, 0));
    scenario
        .stimulate(
            () ->
                unchecked(
                    () ->
                        call(
                                patch("/api/v1/users/" + s.student().id() + "/status"),
                                s.admin(),
                                "{\"status\":\"INACTIVE\"}")
                            .andExpect(status().isOk())))
        .andWaitForStateChange(() -> sessionStatus(future) == SessionStatus.CANCELLED_SYSTEM)
        .andVerify(ok -> assertThat(enrollment(s.enrollmentId()).sessionsReserved()).isZero());
  }

  @Test
  void anonymizationCancelsFutureSessionsIdempotently(Scenario scenario) throws Exception {
    Setup s = setup(4);
    UUID future = bookOk(s.student(), s.enrollmentId(), at(2, 15, 0));
    StudentAnonymized event = new StudentAnonymized(s.student().id(), UUID.randomUUID());
    scenario
        .publish(event)
        .andWaitForStateChange(() -> sessionStatus(future) == SessionStatus.CANCELLED_SYSTEM)
        .andVerify(ok -> assertThat(enrollment(s.enrollmentId()).sessionsReserved()).isZero());
    scenario
        .publish(event)
        .andWaitForStateChange(() -> enrollment(s.enrollmentId()))
        .andVerify(e -> assertThat(e.sessionsReserved()).isZero());
  }

  private SessionStatus sessionStatus(UUID sessionId) {
    return sessions.findById(sessionId).orElseThrow().getStatus();
  }

  /** Buổi đã học 3 ngày trước (tạo thẳng, bộ đếm đổi qua EnrollmentApi như luồng thật). */
  private UUID pastSession(Setup s) {
    UUID[] id = new UUID[1];
    inTx(
        () -> {
          Session past =
              sessions.save(
                  new Session(
                      s.enrollmentId(),
                      s.teacher().id(),
                      s.student().id(),
                      at(-3, 10, 0),
                      45,
                      s.student().id(),
                      null));
          enrollmentApi.reserve(s.enrollmentId());
          past.recordOutcome(SessionStatus.COMPLETED, s.teacher().id(), at(-3, 11, 0));
          enrollmentApi.consume(s.enrollmentId());
          id[0] = past.getId();
        });
    return id[0];
  }
}
