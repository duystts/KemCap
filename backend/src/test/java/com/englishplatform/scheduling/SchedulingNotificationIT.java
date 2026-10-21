package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.notification.EmailSender.EmailMessage;
import com.englishplatform.notification.internal.DispatchService;
import com.englishplatform.notification.internal.FakeEmailSender;
import com.englishplatform.notification.internal.NotificationDispatch;
import com.englishplatform.notification.internal.NotificationDispatchRepository;
import com.englishplatform.notification.internal.NotificationRepository;
import com.englishplatform.scheduling.SchedulingEvents.ReminderWindow;
import com.englishplatform.scheduling.SchedulingEvents.SessionReminderDue;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * U5 với U2/U3 thật: thông báo đi đúng người, không trùng (NFR-REL-05), SMTP lỗi không làm hỏng đặt
 * lịch (NFR-REL-06), đổi giáo viên chỉ một thông báo gộp (FR-006), ẩn danh hóa dọn thông báo
 * (FR-031).
 */
class SchedulingNotificationIT extends SchedulingTestSupport {

  @Autowired FakeEmailSender emailSender;
  @Autowired NotificationRepository notifications;
  @Autowired NotificationDispatchRepository dispatches;
  @Autowired ApplicationEventPublisher publisher;
  @Autowired JdbcTemplate jdbc;
  @Autowired DispatchService dispatchService;

  @BeforeEach
  @AfterEach
  void clearOutbox() {
    emailSender.clear();
  }

  @Test
  void bookingSucceedsWhenSmtpIsDownAndEmailIsRetriedLater() throws Exception {
    Setup s = setup(4);
    emailSender.failNext(1000); // SMTP sập

    String body =
        book(s.student(), s.enrollmentId(), at(2, 10, 0))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID sessionId = UUID.fromString(JsonPath.read(body, "$.id"));

    String key = "session:" + sessionId + ":booked:" + s.student().id();
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(dispatches.findByDedupeKey(key))
                    .get()
                    .extracting(NotificationDispatch::getError)
                    .isNotNull());
    assertThat(to(s.student().email())).isEmpty();

    // SMTP hoạt động lại → lượt gửi lại của job gửi được
    emailSender.clear();
    dispatchService.retry(dispatches.findByDedupeKey(key).orElseThrow().getId());
    assertThat(dispatches.findByDedupeKey(key).orElseThrow().getStatus())
        .isEqualTo(NotificationDispatch.Status.SENT);
    assertThat(to(s.student().email())).hasSize(1);
  }

  @Test
  void sameReminderTwiceSendsOneEmailPerPerson() throws Exception {
    Setup s = setup(4);
    UUID sessionId = bookOk(s.student(), s.enrollmentId(), at(1, 9, 0));
    awaitEmails(s.student().email(), 1);
    awaitEmails(s.teacher().email(), 1);
    emailSender.clear();

    SessionReminderDue due =
        new SessionReminderDue(
            sessionId, s.teacher().id(), s.student().id(), at(1, 9, 0), ReminderWindow.H1);
    inTx(() -> publisher.publishEvent(due));
    inTx(() -> publisher.publishEvent(due));

    awaitEmails(s.student().email(), 1);
    awaitEmails(s.teacher().email(), 1);
    Thread.sleep(500);
    assertThat(emailSender.sent())
        .filteredOn(m -> m.subject().startsWith("Nhắc lịch"))
        .extracting(EmailMessage::to)
        .containsExactlyInAnyOrder(s.student().email(), s.teacher().email());
  }

  @Test
  void teacherReassignmentSendsOneAggregatedEmailInsteadOfOnePerCancelledSession()
      throws Exception {
    Setup s = setup(6);
    bookOk(s.student(), s.enrollmentId(), at(2, 10, 0));
    bookOk(s.student(), s.enrollmentId(), at(4, 10, 0));
    TestUser newTeacher = user(Role.TEACHER);
    awaitEmails(s.student().email(), 2);
    awaitEmails(s.teacher().email(), 2);
    emailSender.clear();

    call(
            put("/api/v1/enrollments/" + s.enrollmentId() + "/teacher"),
            s.admin(),
            "{\"teacherId\":\"" + newTeacher.id() + "\",\"reason\":\"GV nghỉ dài hạn\"}")
        .andExpect(status().isOk());

    awaitEmails(s.student().email(), 1);
    awaitEmails(s.teacher().email(), 1);
    awaitEmails(newTeacher.email(), 1);
    Thread.sleep(500);
    List<EmailMessage> toStudent = to(s.student().email());
    assertThat(toStudent).hasSize(1);
    assertThat(toStudent.get(0).subject()).isEqualTo("Đổi giáo viên phụ trách");
    assertThat(toStudent.get(0).html()).contains("Các buổi đã đặt với giáo viên cũ đã được hủy");
  }

  @Test
  void anonymizationDeletesStudentNotificationsAndMasksRecipient() throws Exception {
    Setup s = setup(4);
    bookOk(s.student(), s.enrollmentId(), at(3, 10, 0));
    awaitEmails(s.student().email(), 1);
    assertThat(notifications.countByUserIdAndReadAtIsNull(s.teacher().id())).isEqualTo(1);

    inTx(() -> publisher.publishEvent(new StudentAnonymized(s.student().id(), UUID.randomUUID())));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(notifications.countByUserIdAndReadAtIsNull(s.student().id())).isZero();
              // thông báo GV nhắc tới học viên cũng bị xóa
              assertThat(
                      jdbc.queryForObject(
                          "select count(*) from notification where about_student_id = ?",
                          Long.class,
                          s.student().id()))
                  .isZero();
              assertThat(
                      jdbc.queryForObject(
                          "select count(*) from notification_dispatch"
                              + " where user_id = ? and recipient <> 'anonymized'",
                          Long.class,
                          s.student().id()))
                  .isZero();
            });
  }

  private void awaitEmails(String email, int count) {
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(to(email)).hasSizeGreaterThanOrEqualTo(count));
  }

  private List<EmailMessage> to(String email) {
    return emailSender.sent().stream().filter(m -> m.to().equals(email)).toList();
  }
}
