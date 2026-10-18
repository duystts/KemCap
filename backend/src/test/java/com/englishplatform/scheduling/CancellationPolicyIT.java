package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Chính sách hủy / đổi lịch theo hạn chót cấu hình (FR-011). */
class CancellationPolicyIT extends SchedulingTestSupport {

  @Test
  void cancelBeforeCutoffRefunds() throws Exception {
    Setup s = setup(4);
    UUID id = bookOk(s.student(), s.enrollmentId(), at(3, 10, 0));
    call(get("/api/v1/sessions/" + id + "/cancel-preview"), s.student())
        .andExpect(jsonPath("$.willConsume").value(false));
    call(post("/api/v1/sessions/" + id + "/cancel"), s.student(), "{\"reason\":\"Bận\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED_ON_TIME"));
    assertThat(enrollment(s.enrollmentId()).sessionsRemaining()).isEqualTo(4);
  }

  @Test
  void cancelAfterCutoffConsumes() throws Exception {
    Setup s = setup(4);
    Instant startsAt = at(3, 10, 0);
    UUID id = bookOk(s.student(), s.enrollmentId(), startsAt);
    clock.set(startsAt.minus(Duration.ofHours(11)));
    call(get("/api/v1/sessions/" + id + "/cancel-preview"), s.student())
        .andExpect(jsonPath("$.willConsume").value(true))
        .andExpect(jsonPath("$.cutoffAt").value(startsAt.minus(Duration.ofHours(12)).toString()));
    call(post("/api/v1/sessions/" + id + "/cancel"), s.student(), "{}")
        .andExpect(jsonPath("$.status").value("CANCELLED_LATE"));
    assertThat(enrollment(s.enrollmentId()).sessionsConsumed()).isEqualTo(1);
    assertThat(enrollment(s.enrollmentId()).sessionsReserved()).isZero();
  }

  @Test
  void teacherCancelIsTeacherAbsentAndRefunds() throws Exception {
    Setup s = setup(4);
    UUID id = bookOk(s.student(), s.enrollmentId(), at(3, 11, 0));
    clock.set(at(3, 10, 0)); // còn 1h — giáo viên hủy vẫn hoàn buổi
    call(post("/api/v1/sessions/" + id + "/cancel"), s.teacher(), "{\"reason\":\"Ốm\"}")
        .andExpect(jsonPath("$.status").value("TEACHER_ABSENT"));
    assertThat(enrollment(s.enrollmentId()).sessionsRemaining()).isEqualTo(4);
  }

  @Test
  void rescheduleOnlyBeforeCutoff() throws Exception {
    Setup s = setup(4);
    Instant startsAt = at(3, 10, 0);
    UUID id = bookOk(s.student(), s.enrollmentId(), startsAt);
    Instant newStart = at(4, 14, 0);
    String moved =
        read(
            call(
                    post("/api/v1/sessions/" + id + "/reschedule"),
                    s.student(),
                    "{\"newStartsAt\":\"" + newStart + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rescheduledFromId").value(id.toString()))
                .andExpect(jsonPath("$.startsAt").value(newStart.toString())),
            "$.id");
    call(get("/api/v1/sessions/" + id), s.student())
        .andExpect(jsonPath("$.status").value("CANCELLED_ON_TIME"));
    assertThat(enrollment(s.enrollmentId()).sessionsReserved()).isEqualTo(1);

    clock.set(newStart.minus(Duration.ofHours(6)));
    call(
            post("/api/v1/sessions/" + moved + "/reschedule"),
            s.student(),
            "{\"newStartsAt\":\"" + at(5, 14, 0) + "\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CANCEL_WINDOW_PASSED"));
    // giáo viên không đổi lịch
    call(
            post("/api/v1/sessions/" + moved + "/reschedule"),
            s.teacher(),
            "{\"newStartsAt\":\"" + at(5, 14, 0) + "\"}")
        .andExpect(status().isForbidden());
  }

  /** Đổi cấu hình triển khai → hạn chót đổi theo (quyết định 2026-10-18). */
  @Nested
  @TestPropertySource(properties = "app.scheduling.cancel-cutoff-hours=48")
  class WithLongerCutoff {

    @Test
    void cutoffFollowsConfiguration() throws Exception {
      Setup s = setup(4);
      Instant startsAt = at(4, 10, 0);
      UUID id = bookOk(s.student(), s.enrollmentId(), startsAt);
      clock.set(startsAt.minus(Duration.ofHours(40)));
      call(get("/api/v1/sessions/" + id + "/cancel-preview"), s.student())
          .andExpect(jsonPath("$.willConsume").value(true));
      call(post("/api/v1/sessions/" + id + "/cancel"), s.student(), "{}")
          .andExpect(jsonPath("$.status").value("CANCELLED_LATE"));
    }
  }
}
