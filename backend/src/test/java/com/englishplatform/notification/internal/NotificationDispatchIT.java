package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.englishplatform.identity.PasswordResetRequested;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.internal.PasswordResetService;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAccountRepository;
import com.englishplatform.support.PostgresContainerConfig;
import com.englishplatform.support.TestClockConfig;
import com.englishplatform.support.TestClockConfig.MutableClock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gửi email tin cậy (NFR-REL-06): thử lại theo backoff, chống gửi trùng; và không để token thô nằm
 * lại trong DB sau khi xử lý (NFR-SEC-03).
 */
@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@Import({PostgresContainerConfig.class, TestClockConfig.class})
class NotificationDispatchIT {

  @Autowired DispatchService dispatchService;
  @Autowired RetryDispatchJob retryJob;
  @Autowired NotificationDispatchRepository dispatches;
  @Autowired FakeEmailSender emailSender;
  @Autowired PasswordResetService passwordResets;
  @Autowired UserAccountRepository users;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired JdbcTemplate jdbc;
  @Autowired MutableClock clock;
  @Autowired ApplicationEvents events;
  @Autowired ApplicationEventPublisher publisher;
  @Autowired PlatformTransactionManager txManager;

  @BeforeEach
  void clearOutbox() {
    emailSender.clear();
  }

  @AfterEach
  void resetClock() {
    clock.reset();
    emailSender.clear();
  }

  @Test
  void retriesWithBackoffThenFails() {
    String key = "test:" + UUID.randomUUID();
    emailSender.failNext(4);
    notify(key);
    assertThat(dispatch(key).getAttempts()).isEqualTo((short) 1);

    for (Duration wait : NotificationDispatch.BACKOFF) {
      // chưa tới giờ thử lại → job bỏ qua
      clock.advance(wait.minusSeconds(1));
      retryJob.run();
      short before = dispatch(key).getAttempts();
      clock.advance(Duration.ofSeconds(1));
      retryJob.run();
      assertThat(dispatch(key).getAttempts()).isEqualTo((short) (before + 1));
    }

    NotificationDispatch failed = dispatch(key);
    assertThat(failed.getStatus()).isEqualTo(NotificationDispatch.Status.FAILED);
    assertThat(failed.getAttempts()).isEqualTo((short) 4);
    assertThat(failed.getBodyHtml()).isEqualTo(NotificationDispatch.SCRUBBED_BODY);
    assertThat(emailSender.sent()).isEmpty();
  }

  @Test
  void retrySucceedsAfterTransientFailure() {
    String key = "test:" + UUID.randomUUID();
    emailSender.failNext(1);
    notify(key);
    assertThat(dispatch(key).getStatus()).isEqualTo(NotificationDispatch.Status.PENDING);

    clock.advance(Duration.ofMinutes(1));
    retryJob.run();

    assertThat(dispatch(key).getStatus()).isEqualTo(NotificationDispatch.Status.SENT);
    assertThat(emailSender.sent()).hasSize(1);
  }

  @Test
  void sameDedupeKeyIsSentOnce() {
    String key = "test:" + UUID.randomUUID();
    notify(key);
    notify(key);

    assertThat(
            jdbc.queryForObject(
                "select count(*) from notification_dispatch where dedupe_key = ?", Long.class, key))
        .isEqualTo(1L);
    assertThat(emailSender.sent()).hasSize(1);
  }

  @Test
  void rawResetTokenIsNotKeptInDatabase() {
    UserAccount user =
        new UserAccount(
            "r" + UUID.randomUUID().toString().substring(0, 8) + "@test.local",
            "Người dùng thử",
            Role.TEACHER,
            null,
            UserStatus.INVITED);
    user.activate(passwordEncoder.encode("Matkhau123"));
    users.save(user);

    passwordResets.requestReset(user.getEmail());
    PasswordResetRequested event =
        events.stream(PasswordResetRequested.class)
            .filter(e -> e.userId().equals(user.getId()))
            .findFirst()
            .orElseThrow();
    String key = "user:" + user.getId() + ":reset:" + event.tokenId();
    String like = "%" + event.resetToken() + "%";

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(dispatches.findByDedupeKey(key))
                  .get()
                  .extracting(NotificationDispatch::getStatus)
                  .isEqualTo(NotificationDispatch.Status.SENT);
              assertThat(
                      jdbc.queryForObject(
                          "select count(*) from event_publication where serialized_event like ?",
                          Long.class,
                          like))
                  .isZero();
            });
    assertThat(emailSender.sent())
        .anySatisfy(m -> assertThat(m.html()).contains(event.resetToken()));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from notification_dispatch"
                    + " where body_html like ? or dedupe_key like ?",
                Long.class,
                like,
                like))
        .isZero();
  }

  /** FR-031 (review U5 R2): email mời/đặt lại mật khẩu cũng bị che sau ẩn danh hóa. */
  @Test
  void anonymizationMasksIdentityEmailsOfStudent() {
    UserAccount student =
        new UserAccount(
            "s" + UUID.randomUUID().toString().substring(0, 8) + "@test.local",
            "Học viên thử",
            Role.STUDENT,
            java.time.LocalDate.of(2000, 1, 1),
            UserStatus.INVITED);
    student.activate(passwordEncoder.encode("Matkhau123"));
    users.save(student);
    emailSender.failNext(1); // để lại một lỗi SMTP có thể chứa email trong cột error
    passwordResets.requestReset(student.getEmail());
    await().atMost(Duration.ofSeconds(10)).until(() -> countByRecipient(student.getEmail()) == 1L);

    new TransactionTemplate(txManager)
        .executeWithoutResult(
            s -> publisher.publishEvent(new StudentAnonymized(student.getId(), UUID.randomUUID())));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(countByRecipient(student.getEmail())).isZero());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from notification_dispatch where user_id = ?"
                    + " and (recipient <> 'anonymized' or error is not null or status = 'PENDING')",
                Long.class,
                student.getId()))
        .isZero();
  }

  private long countByRecipient(String email) {
    return jdbc.queryForObject(
        "select count(*) from notification_dispatch where recipient = ?", Long.class, email);
  }

  private void notify(String key) {
    dispatchService.notify(
        key,
        null,
        "a@test.local",
        "Thử",
        "reset-password",
        Map.of("link", "http://localhost/reset"),
        null);
  }

  private NotificationDispatch dispatch(String key) {
    return dispatches.findByDedupeKey(key).orElseThrow();
  }
}
