package com.englishplatform.catalog.internal.jobs;

import com.englishplatform.catalog.CatalogEvents.EnrollmentRenewalDue;
import com.englishplatform.catalog.CatalogEvents.RenewalReason;
import com.englishplatform.catalog.internal.enrollment.Enrollment;
import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xử lý một ghi danh trong transaction riêng; học viên đang ngừng hoặc đã ẩn danh không bị nhắc.
 */
@Component
class EnrollmentLifecycleStep {

  private final EnrollmentRepository enrollments;
  private final UserDirectory userDirectory;
  private final ApplicationEventPublisher events;
  private final int lowSessions;
  private final Duration expiringWindow;

  EnrollmentLifecycleStep(
      EnrollmentRepository enrollments,
      UserDirectory userDirectory,
      ApplicationEventPublisher events,
      @Value("${app.reminders.renewal-low-sessions}") int lowSessions,
      @Value("${app.reminders.renewal-expiring-days}") int expiringDays) {
    this.enrollments = enrollments;
    this.userDirectory = userDirectory;
    this.events = events;
    this.lowSessions = lowSessions;
    this.expiringWindow = Duration.ofDays(expiringDays);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void process(UUID enrollmentId, Instant now) {
    Enrollment e = enrollments.findById(enrollmentId).orElse(null);
    if (e == null) {
      return;
    }
    if (e.isPastExpiry(now)) {
      e.expire();
      return;
    }
    boolean low = e.needsLowSessionsReminder(lowSessions);
    boolean expiring = e.needsExpiringReminder(now, expiringWindow);
    if ((!low && !expiring) || !canBeReminded(e.getStudentId())) {
      return;
    }
    if (low) {
      e.markLowSessionsReminded(now);
      publish(e, RenewalReason.LOW_SESSIONS);
    }
    if (expiring) {
      e.markExpiringReminded(now);
      publish(e, RenewalReason.EXPIRING);
    }
  }

  /**
   * Đang ngừng thì chưa đánh dấu đã nhắc: kích hoạt lại vẫn nhận được lời nhắc (ẩn danh: INACTIVE).
   */
  private boolean canBeReminded(UUID studentId) {
    return userDirectory
            .find(studentId)
            .filter(student -> student.status() != UserStatus.INACTIVE)
            .isPresent()
        && !userDirectory.isAwaitingGuardianConsent(studentId);
  }

  private void publish(Enrollment e, RenewalReason reason) {
    events.publishEvent(
        new EnrollmentRenewalDue(
            e.getId(), e.getStudentId(), e.sessionsRemaining(), e.getExpiresAt(), reason));
  }
}
