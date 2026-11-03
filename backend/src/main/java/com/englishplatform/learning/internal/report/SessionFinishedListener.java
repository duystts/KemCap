package com.englishplatform.learning.internal.report;

import com.englishplatform.identity.UserDirectory;
import com.englishplatform.scheduling.SchedulingEvents.SessionFinished;
import com.englishplatform.scheduling.SessionQueryApi;
import com.englishplatform.scheduling.SessionQueryApi.SessionView;
import com.englishplatform.scheduling.SessionStatus;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Buổi hoàn thành → phiếu {@code PENDING}, hạn {@code ends_at + 24h}; phát lại không tạo thêm
 * (NFR-REL-11). Giáo vụ sửa kết quả thành không hoàn thành → bỏ phiếu chưa nộp, phiếu đã nộp thì
 * giữ nhưng không tính vào tiến bộ. Đọc trạng thái thật của buổi vì event có thể tới sai thứ tự.
 * Học viên đã ẩn danh không còn dữ liệu để đánh giá nên không tạo phiếu.
 */
@Component
class SessionFinishedListener {

  private final SessionReportRepository reports;
  private final SessionQueryApi sessions;
  private final UserDirectory userDirectory;
  private final Clock clock;
  private final Duration dueAfter;

  SessionFinishedListener(
      SessionReportRepository reports,
      SessionQueryApi sessions,
      UserDirectory userDirectory,
      Clock clock,
      @Value("${app.reminders.report-overdue-hours}") int overdueHours) {
    this.reports = reports;
    this.sessions = sessions;
    this.userDirectory = userDirectory;
    this.clock = clock;
    this.dueAfter = Duration.ofHours(overdueHours);
  }

  @ApplicationModuleListener
  void on(SessionFinished event) {
    SessionView session = sessions.find(event.sessionId()).orElse(null);
    if (session == null) {
      return;
    }
    SessionReport existing = reports.findBySessionId(session.id()).orElse(null);
    if (session.status() != SessionStatus.COMPLETED) {
      if (existing != null && existing.getStatus() == SessionReport.Status.PENDING) {
        reports.delete(existing);
      } else if (existing != null) {
        existing.markSessionVoided(clock.instant());
      }
      return;
    }
    if (existing != null) {
      existing.clearSessionVoided();
    } else if (!userDirectory.isAnonymized(session.studentId())) {
      reports.save(
          new SessionReport(
              session.id(),
              session.teacherId(),
              session.studentId(),
              session.endsAt(),
              session.endsAt().plus(dueAfter)));
    }
  }
}
