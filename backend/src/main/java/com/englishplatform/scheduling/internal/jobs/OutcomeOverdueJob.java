package com.englishplatform.scheduling.internal.jobs;

import com.englishplatform.scheduling.SchedulingEvents.SessionOutcomeOverdue;
import com.englishplatform.scheduling.internal.session.Session;
import com.englishplatform.scheduling.internal.session.SessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mỗi giờ: buổi còn {@code BOOKED} khi {@code now ≥ ends_at + 24h} → {@link SessionOutcomeOverdue}
 * đúng một lần (FR-014), để nhắc giáo viên + admin chốt thay.
 */
@Component
class OutcomeOverdueJob {

  private final SessionRepository sessions;
  private final ApplicationEventPublisher events;
  private final Clock clock;
  private final Duration overdueAfter;

  OutcomeOverdueJob(
      SessionRepository sessions,
      ApplicationEventPublisher events,
      Clock clock,
      @Value("${app.reminders.outcome-overdue-hours}") int overdueHours) {
    this.sessions = sessions;
    this.events = events;
    this.clock = clock;
    this.overdueAfter = Duration.ofHours(overdueHours);
  }

  @Scheduled(cron = "${app.jobs.outcome-overdue-cron:0 0 * * * *}")
  @SchedulerLock(name = "scheduling.outcomeOverdue", lockAtMostFor = "PT5M")
  @Transactional
  public void run() {
    Instant now = clock.instant();
    for (Session s : sessions.findOutcomeOverdue(now.minus(overdueAfter))) {
      if (sessions.markOutcomeOverdueNotified(s.getId(), now) == 1) {
        events.publishEvent(new SessionOutcomeOverdue(s.getId(), s.getTeacherId(), s.getEndsAt()));
      }
    }
  }
}
