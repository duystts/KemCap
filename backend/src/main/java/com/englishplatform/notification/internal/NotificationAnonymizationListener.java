package com.englishplatform.notification.internal;

import com.englishplatform.identity.StudentAnonymized;
import java.time.Clock;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Ẩn danh hóa (FR-031, data-model.md): xóa thông báo của học viên (và thông báo GV/admin nhắc tới
 * học viên), che {@code recipient} + nội dung email đã ghi cho học viên. Idempotent.
 */
@Component
class NotificationAnonymizationListener {

  private final NotificationRepository notifications;
  private final NotificationDispatchRepository dispatches;
  private final Clock clock;

  NotificationAnonymizationListener(
      NotificationRepository notifications,
      NotificationDispatchRepository dispatches,
      Clock clock) {
    this.notifications = notifications;
    this.dispatches = dispatches;
    this.clock = clock;
  }

  @ApplicationModuleListener
  void on(StudentAnonymized event) {
    notifications.deleteForStudent(event.studentId());
    dispatches.anonymizeForUser(
        event.studentId(), NotificationDispatch.SCRUBBED_BODY, clock.instant());
  }
}
