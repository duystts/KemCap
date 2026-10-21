package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.UserView;
import com.englishplatform.scheduling.SchedulingEvents.CancelCause;
import com.englishplatform.scheduling.SchedulingEvents.ReminderWindow;
import com.englishplatform.scheduling.SchedulingEvents.SessionBooked;
import com.englishplatform.scheduling.SchedulingEvents.SessionCancelled;
import com.englishplatform.scheduling.SchedulingEvents.SessionOutcomeOverdue;
import com.englishplatform.scheduling.SchedulingEvents.SessionReminderDue;
import com.englishplatform.scheduling.SessionQueryApi;
import com.englishplatform.scheduling.SessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** T132: đúng người nhận theo người hủy/lý do, đúng múi giờ, dedupe theo người nhận. */
class SchedulingNotificationListenerTest {

  private static final Instant STARTS = Instant.parse("2026-11-01T11:00:00Z");

  private final UUID studentId = UUID.randomUUID();
  private final UUID teacherId = UUID.randomUUID();
  private final UUID adminId = UUID.randomUUID();
  private final UUID sessionId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();

  private DispatchService dispatch;
  private SessionQueryApi sessionQueryApi;
  private SchedulingNotificationListener listener;

  @BeforeEach
  void setUp() {
    dispatch = mock(DispatchService.class);
    UserDirectory users = mock(UserDirectory.class);
    sessionQueryApi = mock(SessionQueryApi.class);
    listener =
        new SchedulingNotificationListener(dispatch, users, sessionQueryApi, "https://app.test");
    when(users.find(studentId))
        .thenReturn(Optional.of(user(studentId, Role.STUDENT, "Asia/Ho_Chi_Minh")));
    // giáo viên ở London để kiểm giờ theo múi giờ người nhận
    when(users.find(teacherId))
        .thenReturn(Optional.of(user(teacherId, Role.TEACHER, "Europe/London")));
    when(users.findAdmins()).thenReturn(List.of(user(adminId, Role.ADMIN, null)));
    when(sessionQueryApi.find(sessionId))
        .thenReturn(
            Optional.of(
                new SessionQueryApi.SessionView(
                    sessionId,
                    enrollmentId,
                    teacherId,
                    studentId,
                    STARTS,
                    STARTS.plusSeconds(3600),
                    SessionStatus.BOOKED)));
  }

  @Test
  void bookedNotifiesBothInTheirOwnTimezone() {
    listener.on(
        new SessionBooked(
            sessionId, enrollmentId, teacherId, studentId, STARTS, STARTS.plusSeconds(3600)));

    Sent sent = captured("session-booked", 2);
    assertThat(sent.users()).containsExactly(studentId, teacherId);
    assertThat(sent.keys())
        .containsExactly(
            "session:" + sessionId + ":booked:" + studentId,
            "session:" + sessionId + ":booked:" + teacherId);
    assertThat(sent.models().get(0)).containsEntry("formattedTime", "18:00, 01/11/2026");
    assertThat(sent.models().get(1)).containsEntry("formattedTime", "11:00, 01/11/2026");
    assertThat(sent.models().get(0))
        .containsEntry("link", "https://app.test/sessions/" + sessionId);
  }

  @Test
  void reminderDedupesPerWindowAndUser() {
    listener.on(new SessionReminderDue(sessionId, teacherId, studentId, STARTS, ReminderWindow.H1));

    assertThat(captured("session-reminder", 2).keys())
        .containsExactly(
            "session:" + sessionId + ":reminder:H1:" + studentId,
            "session:" + sessionId + ":reminder:H1:" + teacherId);
  }

  @Test
  void studentCancelNotifiesOnlyTeacher() {
    listener.on(cancelled(SessionStatus.CANCELLED_ON_TIME, studentId, null));

    assertThat(captured("session-cancelled", 1).users()).containsExactly(teacherId);
  }

  @Test
  void teacherAbsentNotifiesStudentAndAdminsButNotTeacher() {
    listener.on(cancelled(SessionStatus.TEACHER_ABSENT, teacherId, null));

    Sent sent = captured("session-cancelled", 2);
    assertThat(sent.users()).containsExactly(studentId, adminId);
    assertThat(sent.models().get(1)).containsKey("consumeNote");
  }

  @Test
  void adminLateCancelNotifiesBothAndWarnsStudentAboutConsumption() {
    listener.on(cancelled(SessionStatus.CANCELLED_LATE, adminId, null));

    Sent sent = captured("session-cancelled", 2);
    assertThat(sent.users()).containsExactly(studentId, teacherId);
    assertThat(sent.models().get(0).get("consumeNote")).isNotNull();
    assertThat(sent.models().get(1).get("consumeNote")).isNull();
  }

  @Test
  void reassignmentCancelIsSkippedBecauseItIsAggregated() {
    listener.on(cancelled(SessionStatus.CANCELLED_SYSTEM, null, CancelCause.TEACHER_REASSIGNED));

    verify(dispatch, never()).notify(any(), any(), any(), any(), any(), anyMap(), any());
  }

  @Test
  void anonymizedCancelNotifiesOnlyTeacher() {
    listener.on(cancelled(SessionStatus.CANCELLED_SYSTEM, null, CancelCause.ANONYMIZED));

    assertThat(captured("session-cancelled", 1).users()).containsExactly(teacherId);
  }

  @Test
  void deactivationCancelNotifiesBoth() {
    listener.on(cancelled(SessionStatus.CANCELLED_SYSTEM, null, CancelCause.USER_DEACTIVATED));

    Sent sent = captured("session-cancelled", 2);
    assertThat(sent.users()).containsExactly(studentId, teacherId);
    assertThat(sent.models().get(0)).containsEntry("reason", "Tài khoản học viên ngừng hoạt động");
  }

  @Test
  void outcomeOverdueNotifiesTeacherAndAdmins() {
    listener.on(new SessionOutcomeOverdue(sessionId, teacherId, STARTS.plusSeconds(3600)));

    Sent sent = captured("session-outcome-overdue", 2);
    assertThat(sent.users()).containsExactly(teacherId, adminId);
    // admin không có timezone → mặc định giờ Việt Nam
    assertThat(sent.models().get(1)).containsEntry("formattedTime", "19:00, 01/11/2026");
  }

  private SessionCancelled cancelled(SessionStatus status, UUID by, CancelCause cause) {
    return new SessionCancelled(sessionId, enrollmentId, status, by, STARTS, cause);
  }

  @SuppressWarnings("unchecked")
  private Sent captured(String template, int times) {
    ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<UUID> users = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<Map<String, Object>> models = ArgumentCaptor.forClass(Map.class);
    verify(dispatch, org.mockito.Mockito.times(times))
        .notify(
            keys.capture(), users.capture(), any(), any(), eq(template), models.capture(), any());
    return new Sent(keys.getAllValues(), users.getAllValues(), models.getAllValues());
  }

  private record Sent(List<String> keys, List<UUID> users, List<Map<String, Object>> models) {}

  static UserView user(UUID id, Role role, String timezone) {
    return new UserView(
        id,
        role.name().toLowerCase() + "@test.local",
        "Tên " + role,
        role,
        UserStatus.ACTIVE,
        timezone);
  }
}
