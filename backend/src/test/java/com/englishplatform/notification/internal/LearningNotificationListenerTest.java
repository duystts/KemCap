package com.englishplatform.notification.internal;

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
import com.englishplatform.learning.LearningEvents;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LearningNotificationListenerTest {

  private DispatchService dispatchService;
  private UserDirectory userDirectory;
  private LearningNotificationListener listener;

  private final UUID studentId = UUID.randomUUID();
  private final UUID teacherId = UUID.randomUUID();
  private final UUID sessionId = UUID.randomUUID();
  private final UUID assignmentId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    dispatchService = mock(DispatchService.class);
    userDirectory = mock(UserDirectory.class);
    listener = new LearningNotificationListener(dispatchService, userDirectory, "http://localhost:5173");

    UserView student =
        new UserView(studentId, "student@test.local", "Học Viên", Role.STUDENT, UserStatus.ACTIVE, "Asia/Ho_Chi_Minh");
    UserView teacher =
        new UserView(teacherId, "teacher@test.local", "Giáo Viên", Role.TEACHER, UserStatus.ACTIVE, "Asia/Ho_Chi_Minh");

    when(userDirectory.find(studentId)).thenReturn(Optional.of(student));
    when(userDirectory.find(teacherId)).thenReturn(Optional.of(teacher));
  }

  @Test
  void onSessionReportOverdue_notifiesTeacher() {
    listener.on(new LearningEvents.SessionReportOverdue(sessionId, teacherId, Instant.now()));

    verify(dispatchService)
        .notify(eq("learning:report-overdue:" + sessionId), eq(teacherId), any(), any(), eq("session-report-overdue"), anyMap(), any());
  }

  @Test
  void onSessionReportSubmitted_notifiesStudent() {
    listener.on(new LearningEvents.SessionReportSubmitted(sessionId, studentId));

    verify(dispatchService)
        .notify(eq("learning:report-submitted:" + sessionId), eq(studentId), any(), any(), eq("session-report-submitted"), anyMap(), any());
  }

  @Test
  void onAssignmentSubmitted_notifiesTeacher() {
    listener.on(new LearningEvents.AssignmentSubmitted(assignmentId, studentId, teacherId));

    verify(dispatchService)
        .notify(eq("learning:assignment-submitted:" + assignmentId), eq(teacherId), any(), any(), eq("assignment-submitted"), anyMap(), any());
  }

  @Test
  void onAssignmentGraded_notifiesStudent() {
    listener.on(new LearningEvents.AssignmentGraded(assignmentId, studentId, 10));

    verify(dispatchService)
        .notify(eq("learning:assignment-graded:" + assignmentId), eq(studentId), any(), any(), eq("assignment-graded"), anyMap(), any());
  }
}
