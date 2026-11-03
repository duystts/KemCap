package com.englishplatform.scheduling;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Số buổi đã dạy (FR-027): đúng trạng thái được tính, đúng kỳ theo ngày giờ Việt Nam. */
class TeacherSessionsReportIT extends SchedulingTestSupport {

  @Autowired JdbcTemplate jdbc;

  private static String path(LocalDate from, LocalDate to) {
    return "/api/v1/reports/teacher-sessions?from=" + from + "&to=" + to;
  }

  private String row(UUID teacherId) {
    return "$[?(@.teacherId == '" + teacherId + "')]";
  }

  @Test
  void countsOnlyTaughtStatusesPerTeacher() throws Exception {
    Setup s = setup(12);
    Setup other = setup(4);
    List<String> statuses =
        List.of(
            "COMPLETED",
            "STUDENT_NO_SHOW",
            "CANCELLED_LATE",
            "CANCELLED_SYSTEM",
            "CANCELLED_ON_TIME",
            "TEACHER_ABSENT",
            "INTERRUPTED",
            "BOOKED");
    LocalDate day = LocalDate.now(VN).plusDays(3);
    for (int i = 0; i < statuses.size(); i++) {
      UUID id = bookOk(s.student(), s.enrollmentId(), at(3, 8 + i, 0));
      jdbc.update("update session set status = ? where id = ?", statuses.get(i), id);
    }
    UUID othersSession = bookOk(other.student(), other.enrollmentId(), at(3, 9, 0));
    jdbc.update("update session set status = 'COMPLETED' where id = ?", othersSession);

    call(get(path(day, day)), s.admin())
        .andExpect(status().isOk())
        .andExpect(jsonPath(row(s.teacher().id()) + ".sessionsTaught").value(3))
        .andExpect(jsonPath(row(s.teacher().id()) + ".teacherName").value("Người dùng TEACHER"))
        .andExpect(jsonPath(row(other.teacher().id()) + ".sessionsTaught").value(1));
  }

  /** 06:00 giờ VN là 23:00 UTC ngày hôm trước: kỳ phải tính theo ngày giờ VN, không theo UTC. */
  @Test
  void rangeUsesVietnamDatesInclusive() throws Exception {
    Setup s = setup(4);
    Instant earlyMorning = at(3, 6, 0);
    UUID early = bookOk(s.student(), s.enrollmentId(), earlyMorning);
    UUID late = bookOk(s.student(), s.enrollmentId(), at(5, 21, 0));
    jdbc.update("update session set status = 'COMPLETED' where id in (?, ?)", early, late);
    LocalDate d3 = LocalDate.now(VN).plusDays(3);
    LocalDate d5 = LocalDate.now(VN).plusDays(5);

    call(get(path(d3, d3)), s.admin())
        .andExpect(jsonPath(row(s.teacher().id()) + ".sessionsTaught").value(1));
    call(get(path(d3.minusDays(1), d3.minusDays(1))), s.admin())
        .andExpect(jsonPath(row(s.teacher().id())).isEmpty());
    call(get(path(d3, d5)), s.admin())
        .andExpect(jsonPath(row(s.teacher().id()) + ".sessionsTaught").value(2));
    call(get(path(d5.plusDays(1), d5.plusDays(1))), s.admin())
        .andExpect(jsonPath(row(s.teacher().id())).isEmpty());
  }

  @Test
  void onlyAdminReadsReportAndRangeIsValidated() throws Exception {
    Setup s = setup(2);
    String today = LocalDate.now(VN).toString();
    String query = "/api/v1/reports/teacher-sessions?from=" + today + "&to=" + today;
    call(get(query), s.teacher()).andExpect(status().isForbidden());
    call(get(query), s.student()).andExpect(status().isForbidden());
    call(get("/api/v1/reports/teacher-sessions?from=2026-02-01&to=2026-01-01"), s.admin())
        .andExpect(status().isBadRequest());
    call(get("/api/v1/reports/teacher-sessions"), user(Role.ADMIN)).andExpect(status().isOk());
  }
}
