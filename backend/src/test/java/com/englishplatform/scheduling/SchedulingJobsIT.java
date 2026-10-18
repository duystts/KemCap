package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.scheduling.SchedulingEvents.ReminderWindow;
import com.englishplatform.scheduling.SchedulingEvents.SessionOutcomeOverdue;
import com.englishplatform.scheduling.SchedulingEvents.SessionReminderDue;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** Job nhắc lịch / quá hạn chốt với TestClock: không phát trùng (FR-013, FR-014); lịch rảnh. */
@RecordApplicationEvents
class SchedulingJobsIT extends SchedulingTestSupport {

  @Autowired ApplicationEvents events;
  @Autowired ApplicationContext context;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

  @Test
  void remindersFireOncePerWindow() throws Exception {
    Setup s = setup(4);
    Instant startsAt = at(3, 10, 0);
    UUID id = bookOk(s.student(), s.enrollmentId(), startsAt);

    clock.set(startsAt.minus(Duration.ofHours(23)));
    runJob("sessionReminderJob");
    runJob("sessionReminderJob");
    assertThat(reminders(id, ReminderWindow.H24)).isEqualTo(1);
    assertThat(reminders(id, ReminderWindow.H1)).isZero();

    clock.set(startsAt.minus(Duration.ofMinutes(50)));
    runJob("sessionReminderJob");
    runJob("sessionReminderJob");
    assertThat(reminders(id, ReminderWindow.H1)).isEqualTo(1);
    assertThat(reminders(id, ReminderWindow.H24)).isEqualTo(1);
  }

  @Test
  void sessionBookedInsideWindowSkipsThe24hReminder() throws Exception {
    Setup s = setup(4);
    Instant startsAt = at(3, 10, 0);
    UUID id = bookOk(s.student(), s.enrollmentId(), startsAt);
    // giả lập buổi được đặt khi chỉ còn 5h (BaseEntity chưa dùng Clock — R18, U5)
    jdbc.update(
        "update session set created_at = ? where id = ?",
        java.sql.Timestamp.from(startsAt.minus(Duration.ofHours(5))),
        id);
    clock.set(startsAt.minus(Duration.ofHours(5)));
    runJob("sessionReminderJob");
    assertThat(reminders(id, ReminderWindow.H24)).isZero();
    clock.set(startsAt.minus(Duration.ofMinutes(30)));
    runJob("sessionReminderJob");
    assertThat(reminders(id, ReminderWindow.H1)).isEqualTo(1);
  }

  @Test
  void outcomeOverdueFiresOnce() throws Exception {
    Setup s = setup(4);
    Instant startsAt = at(2, 10, 0);
    UUID id = bookOk(s.student(), s.enrollmentId(), startsAt);
    clock.set(startsAt.plus(Duration.ofHours(20)));
    runJob("outcomeOverdueJob");
    assertThat(overdue(id)).isZero();
    clock.set(startsAt.plus(Duration.ofHours(26)));
    runJob("outcomeOverdueJob");
    runJob("outcomeOverdueJob");
    assertThat(overdue(id)).isEqualTo(1);
  }

  @Test
  void availabilityRulesAndExceptions() throws Exception {
    Setup s = setup(4);
    String url = "/api/v1/teachers/" + s.teacher().id() + "/availability";
    call(
            put(url),
            s.teacher(),
            "[{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"18:00\",\"endTime\":\"21:00\"},"
                + "{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"20:00\",\"endTime\":\"22:00\"}]")
        .andExpect(status().isBadRequest());
    call(put(url), user(com.englishplatform.identity.Role.TEACHER), "[]")
        .andExpect(status().isNotFound());

    // lịch rảnh mọi ngày từ setup → đặt một buổi rồi xin nghỉ trùng ngày đó
    Instant startsAt = at(4, 10, 0);
    UUID id = bookOk(s.student(), s.enrollmentId(), startsAt);
    LocalDate day = startsAt.atZone(VN).toLocalDate();
    call(post(url + "/exceptions"), s.teacher(), "{\"date\":\"" + day + "\",\"reason\":\"Ốm\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EXCEPTION_OVERLAPS_SESSIONS"))
        .andExpect(jsonPath("$.sessions[0].id").value(id.toString()));
    call(
            post(url + "/exceptions"),
            s.teacher(),
            "{\"date\":\"" + day + "\",\"startTime\":\"14:00\",\"endTime\":\"16:00\"}")
        .andExpect(status().isCreated());
  }

  private void runJob(String bean) throws Exception {
    Object job = context.getBean(bean);
    job.getClass().getMethod("run").invoke(job);
  }

  private long reminders(UUID sessionId, ReminderWindow window) {
    return events.stream(SessionReminderDue.class)
        .filter(e -> e.sessionId().equals(sessionId) && e.window() == window)
        .count();
  }

  private long overdue(UUID sessionId) {
    return events.stream(SessionOutcomeOverdue.class)
        .filter(e -> e.sessionId().equals(sessionId))
        .count();
  }
}
