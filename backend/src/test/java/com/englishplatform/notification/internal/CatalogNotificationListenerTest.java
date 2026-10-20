package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishplatform.catalog.CatalogEvents;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.UserView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CatalogNotificationListenerTest {

  private DispatchService dispatchService;
  private UserDirectory userDirectory;
  private CatalogNotificationListener listener;

  private final UUID studentId = UUID.randomUUID();
  private final UUID teacherId1 = UUID.randomUUID();
  private final UUID teacherId2 = UUID.randomUUID();
  private final UUID adminId = UUID.randomUUID();
  private final UUID enrollmentId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    dispatchService = mock(DispatchService.class);
    userDirectory = mock(UserDirectory.class);
    listener = new CatalogNotificationListener(dispatchService, userDirectory, "http://localhost:5173");

    UserView student =
        new UserView(studentId, "student@test.local", "Học Viên", Role.STUDENT, UserStatus.ACTIVE, "Asia/Ho_Chi_Minh");
    UserView teacher1 =
        new UserView(teacherId1, "teacher1@test.local", "Giáo Viên 1", Role.TEACHER, UserStatus.ACTIVE, "Asia/Ho_Chi_Minh");
    UserView teacher2 =
        new UserView(teacherId2, "teacher2@test.local", "Giáo Viên 2", Role.TEACHER, UserStatus.ACTIVE, "Asia/Ho_Chi_Minh");
    UserView admin =
        new UserView(adminId, "admin@test.local", "Giáo Vụ", Role.ADMIN, UserStatus.ACTIVE, "Asia/Ho_Chi_Minh");

    when(userDirectory.find(studentId)).thenReturn(Optional.of(student));
    when(userDirectory.find(teacherId1)).thenReturn(Optional.of(teacher1));
    when(userDirectory.find(teacherId2)).thenReturn(Optional.of(teacher2));
    when(userDirectory.findAdmins()).thenReturn(List.of(admin));
  }

  @Test
  void onTeacherReassigned_notifiesThreeParties() {
    listener.on(new CatalogEvents.TeacherReassigned(enrollmentId, studentId, teacherId1, teacherId2));

    verify(dispatchService, times(3))
        .notify(any(), any(), any(), any(), eq("teacher-reassigned"), anyMap(), any());
  }

  @Test
  void onEnrollmentActivated_notifiesStudent() {
    Instant expiresAt = Instant.parse("2026-12-31T00:00:00Z");
    listener.on(new CatalogEvents.EnrollmentActivated(enrollmentId, studentId, expiresAt));

    ArgumentCaptor<String> dedupeCaptor = ArgumentCaptor.forClass(String.class);
    verify(dispatchService)
        .notify(dedupeCaptor.capture(), eq(studentId), any(), any(), eq("enrollment-activated"), anyMap(), any());

    assertThat(dedupeCaptor.getValue()).isEqualTo("enrollment:" + enrollmentId + ":activated");
  }

  @Test
  void onEnrollmentRenewalDue_notifiesStudentAndAdmins() {
    Instant expiresAt = Instant.parse("2026-12-31T00:00:00Z");
    listener.on(
        new CatalogEvents.EnrollmentRenewalDue(
            enrollmentId, studentId, 2, expiresAt, CatalogEvents.RenewalReason.LOW_SESSIONS));

    // 1 student + 1 admin = 2 notifications
    verify(dispatchService, times(2))
        .notify(any(), any(), any(), any(), eq("renewal-reminder"), anyMap(), any());
  }
}
