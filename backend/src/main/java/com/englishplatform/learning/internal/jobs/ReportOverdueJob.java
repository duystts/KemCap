package com.englishplatform.learning.internal.jobs;

import com.englishplatform.learning.LearningEvents.SessionReportOverdue;
import com.englishplatform.learning.internal.report.SessionReport;
import com.englishplatform.learning.internal.report.SessionReportRepository;
import java.time.Clock;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Mỗi giờ: phiếu chưa nộp quá hạn → nhắc giáo viên đúng một lần (FR-019). */
@Component
public class ReportOverdueJob {

  private final SessionReportRepository reports;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  ReportOverdueJob(SessionReportRepository reports, ApplicationEventPublisher events, Clock clock) {
    this.reports = reports;
    this.events = events;
    this.clock = clock;
  }

  @Scheduled(cron = "${app.jobs.report-overdue-cron:0 0 * * * *}")
  @SchedulerLock(name = "learning.reportOverdue", lockAtMostFor = "PT5M")
  @Transactional
  public void run() {
    Instant now = clock.instant();
    for (SessionReport r : reports.findOverdue(now)) {
      if (reports.markOverdueNotified(r.getId(), now) == 1) {
        events.publishEvent(
            new SessionReportOverdue(r.getSessionId(), r.getTeacherId(), r.getDueAt()));
      }
    }
  }
}
