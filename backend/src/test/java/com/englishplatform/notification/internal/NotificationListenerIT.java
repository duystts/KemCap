package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishplatform.catalog.CatalogEvents;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.identity.internal.UserAccountRepository;
import com.englishplatform.learning.LearningEvents;
import com.englishplatform.scheduling.SchedulingEvents;
import com.englishplatform.support.PostgresContainerConfig;
import com.englishplatform.support.TestClockConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresContainerConfig.class, TestClockConfig.class})
class NotificationListenerIT {

  @Autowired ApplicationEventPublisher events;
  @Autowired FakeEmailSender emailSender;
  @Autowired NotificationRepository notificationRepository;
  @Autowired NotificationDispatchRepository dispatchRepository;
  @Autowired UserAccountRepository userAccountRepository;

  private UserAccount student;
  private UserAccount teacher;
  private UserAccount admin;

  @BeforeEach
  void setUp() {
    emailSender.clear();

    student =
        userAccountRepository.save(
            new UserAccount(
                "student.test@demo.local",
                "Học Viên Test",
                Role.STUDENT,
                LocalDate.of(2000, 1, 1),
                UserStatus.ACTIVE));

    teacher =
        userAccountRepository.save(
            new UserAccount(
                "teacher.test@demo.local",
                "Giáo Viên Test",
                Role.TEACHER,
                null,
                UserStatus.ACTIVE));

    admin =
        userAccountRepository.save(
            new UserAccount(
                "admin.test@demo.local", "Giáo Vụ Test", Role.ADMIN, null, UserStatus.ACTIVE));
  }

  @AfterEach
  void tearDown() {
    emailSender.clear();
  }

  @Test
  void sessionBooked_sendsEmailAndInAppToBoth() {
    UUID sessionId = UUID.randomUUID();
    UUID enrollmentId = UUID.randomUUID();
    Instant startsAt = Instant.parse("2026-11-01T10:00:00Z");
    Instant endsAt = Instant.parse("2026-11-01T11:00:00Z");

    events.publishEvent(
        new SchedulingEvents.SessionBooked(
            sessionId, enrollmentId, teacher.getId(), student.getId(), startsAt, endsAt));

    assertThat(emailSender.sent())
        .hasSize(2)
        .anyMatch(msg -> msg.to().equals(student.getEmail()))
        .anyMatch(msg -> msg.to().equals(teacher.getEmail()));

    assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(student.getId(), null))
        .anyMatch(n -> n.getType().equals("SESSION_BOOKED"));
    assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(teacher.getId(), null))
        .anyMatch(n -> n.getType().equals("SESSION_BOOKED"));
  }

  @Test
  void sessionReminder_deduplicatesCorrectly() {
    UUID sessionId = UUID.randomUUID();
    Instant startsAt = Instant.parse("2026-11-01T10:00:00Z");

    SchedulingEvents.SessionReminderDue reminder =
        new SchedulingEvents.SessionReminderDue(
            sessionId,
            teacher.getId(),
            student.getId(),
            startsAt,
            SchedulingEvents.ReminderWindow.H24);

    // Bắn lần 1 -> gửi 2 email
    events.publishEvent(reminder);
    assertThat(emailSender.sent()).hasSize(2);

    // Bắn lần 2 cùng event -> dedupe key chặn lại, không gửi thêm
    events.publishEvent(reminder);
    assertThat(emailSender.sent()).hasSize(2);
  }

  @Test
  void enrollmentActivated_notifiesStudent() {
    UUID enrollmentId = UUID.randomUUID();
    Instant expiresAt = Instant.parse("2026-12-31T23:59:59Z");

    events.publishEvent(
        new CatalogEvents.EnrollmentActivated(enrollmentId, student.getId(), expiresAt));

    assertThat(emailSender.sent())
        .hasSize(1)
        .anyMatch(
            msg -> msg.to().equals(student.getEmail()) && msg.subject().contains("kích hoạt"));

    assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(student.getId(), null))
        .anyMatch(n -> n.getType().equals("ENROLLMENT_ACTIVATED"));
  }

  @Test
  void teacherReassigned_notifiesStudentOldAndNewTeacher() {
    UserAccount newTeacher =
        userAccountRepository.save(
            new UserAccount(
                "teacher.new@demo.local", "Giáo Viên Mới", Role.TEACHER, null, UserStatus.ACTIVE));

    UUID enrollmentId = UUID.randomUUID();

    events.publishEvent(
        new CatalogEvents.TeacherReassigned(
            enrollmentId, student.getId(), teacher.getId(), newTeacher.getId()));

    assertThat(emailSender.sent())
        .hasSize(3)
        .anyMatch(msg -> msg.to().equals(student.getEmail()))
        .anyMatch(msg -> msg.to().equals(teacher.getEmail()))
        .anyMatch(msg -> msg.to().equals(newTeacher.getEmail()));
  }

  @Test
  void learningEvents_notifyTargetUsers() {
    UUID sessionId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();

    // 1. SessionReportOverdue -> Báo teacher
    events.publishEvent(
        new LearningEvents.SessionReportOverdue(
            sessionId, teacher.getId(), Instant.parse("2026-11-01T12:00:00Z")));
    assertThat(emailSender.sent())
        .anyMatch(
            msg -> msg.to().equals(teacher.getEmail()) && msg.subject().contains("phiếu đánh giá"));

    // 2. AssignmentGraded -> Báo student
    events.publishEvent(new LearningEvents.AssignmentGraded(assignmentId, student.getId(), 9));
    assertThat(emailSender.sent())
        .anyMatch(
            msg -> msg.to().equals(student.getEmail()) && msg.subject().contains("chấm điểm"));
  }
}
