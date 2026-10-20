package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.UserView;
import com.englishplatform.scheduling.SchedulingEvents;
import com.englishplatform.scheduling.SessionQueryApi;
import com.englishplatform.scheduling.SessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SchedulingNotificationListenerTest {

  private DispatchService dispatchService;
  private UserDirectory userDirectory;
  private SessionQueryApi sessionQueryApi;
  private SchedulingNotificationListener listener;

  private final UUID studentId = UUID.randomUUID();
  private final UUID teacherId = UUID.randomUUID();
  private final UUID adminId = UUID.randomUUID();
  private final UUID sessionId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    dispatchService = mock(DispatchService.class);
    userDirectory = mock(UserDirectory.class);
    sessionQueryApi = mock(SessionQueryApi.class);
    listener =
        new SchedulingNotificationListener(
            dispatchService, userDirectory, sessionQueryApi, "http://localhost:5173");

    UserView student =
        new UserView(
            studentId, "student@test.local", "Học Viên A", Role.STUDENT, UserStatus.ACTIVE, "Asia/Ho_Chi_Minh");
    UserView teacher =
        new UserView(
            teacherId, "teacher@test.local", "Giáo Viên B", Role.TEACHER, UserStatus.ACTIVE, "Asia/Ho_Chi_Minh");
    UserView admin =
        new UserView(
            adminId, "admin@test.local", "Giáo Vụ C", Role.ADMIN, UserStatus.ACTIVE, "Asia/Ho_Chi_Minh");

    when(userDirectory.find(studentId)).thenReturn(Optional.of(student));
    when(userDirectory.find(teacherId)).thenReturn(Optional.of(teacher));
    when(userDirectory.findAdmins()).thenReturn(List.of(admin));
  }

  @Test
  void onSessionBooked_notifiesBothStudentAndTeacher() {
    Instant startsAt = Instant.parse("2026-11-01T10:00:00Z");
    Instant endsAt = Instant.parse("2026-11-01T11:00:00Z");

    listener.on(
        new SchedulingEvents.SessionBooked(
            sessionId, enrollmentId, teacherId, studentId, startsAt, endsAt));

    ArgumentCaptor<String> dedupeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<UUID> userCaptor = ArgumentCaptor.forClass(UUID.class);

    verify(dispatchService, times(2))
        .notify(
            dedupeCaptor.capture(),
            userCaptor.capture(),
            any(),
            any(),
            eq("session-booked"),
            anyMap(),
            any());

    assertThat(dedupeCaptor.getAllValues())
        .contains(
            "session:" + sessionId + ":booked:student:" + studentId,
            "session:" + sessionId + ":booked:teacher:" + teacherId);
    assertThat(userCaptor.getAllValues()).contains(studentId, teacherId);
  }

  @Test
  void onSessionReminderDue_sendsReminderWithWindow() {
    Instant startsAt = Instant.parse("2026-11-01T10:00:00Z");

    listener.on(
        new SchedulingEvents.SessionReminderDue(
            sessionId, teacherId, studentId, startsAt, SchedulingEvents.ReminderWindow.H24));

    ArgumentCaptor<String> dedupeCaptor = ArgumentCaptor.forClass(String.class);
    verify(dispatchService, times(2))
        .notify(
            dedupeCaptor.capture(),
            any(),
            any(),
            any(),
            eq("session-reminder"),
            anyMap(),
            any());

    assertThat(dedupeCaptor.getAllValues())
        .contains(
            "session:" + sessionId + ":reminder:H24:" + studentId,
            "session:" + sessionId + ":reminder:H24:" + teacherId);
  }

  @Test
  void onSessionCancelled_whenTeacherAbsent_notifiesAdminsToo() {
    Instant startsAt = Instant.parse("2026-11-01T10:00:00Z");
    when(sessionQueryApi.find(sessionId))
        .thenReturn(
            Optional.of(
                new SessionQueryApi.SessionView(
                    sessionId,
                    enrollmentId,
                    teacherId,
                    studentId,
                    startsAt,
                    startsAt.plusSeconds(3600),
                    SessionStatus.TEACHER_ABSENT)));

    listener.on(
        new SchedulingEvents.SessionCancelled(
            sessionId, enrollmentId, SessionStatus.TEACHER_ABSENT, teacherId, startsAt));

    // Báo student + báo teacher + báo admin = 3 thông báo
    verify(dispatchService, times(3))
        .notify(
            any(),
            any(),
            any(),
            any(),
            eq("session-cancelled"),
            anyMap(),
            any());
  }
}
