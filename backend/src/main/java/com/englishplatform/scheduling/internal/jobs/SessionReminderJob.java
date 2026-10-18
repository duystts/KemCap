package com.englishplatform.scheduling.internal.jobs;

import com.englishplatform.scheduling.SchedulingEvents.ReminderWindow;
import com.englishplatform.scheduling.SchedulingEvents.SessionReminderDue;
import com.englishplatform.scheduling.internal.session.Session;
import com.englishplatform.scheduling.internal.session.SessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mỗi phút: nhắc lịch trước 24h và 1h (FR-013). Cột {@code reminder_*_at} chống phát lại; buổi được
 * đặt sau mốc nhắc (vd. đặt lúc còn 5h) chỉ được đánh dấu, không gửi nhắc 24h.
 */
@Component
class SessionReminderJob {

  static final Duration H24 = Duration.ofHours(24);
  static final Duration H1 = Duration.ofHours(1);

  private final SessionRepository sessions;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  SessionReminderJob(SessionRepository sessions, ApplicationEventPublisher events, Clock clock) {
    this.sessions = sessions;
    this.events = events;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "PT60S", initialDelayString = "${app.jobs.initial-delay}")
  @SchedulerLock(name = "scheduling.sessionReminder", lockAtMostFor = "PT5M")
  @Transactional
  public void run() {
    Instant now = clock.instant();
    for (Session s : sessions.findStartingBetween(now, now.plus(H24))) {
      if (s.getReminder24hAt() == null
          && sessions.markReminder24h(s.getId(), now) == 1
          && bookedBefore(s, H24)) {
        publish(s, ReminderWindow.H24);
      }
      if (s.getReminder1hAt() == null
          && !s.getStartsAt().isAfter(now.plus(H1))
          && sessions.markReminder1h(s.getId(), now) == 1
          && bookedBefore(s, H1)) {
        publish(s, ReminderWindow.H1);
      }
    }
  }

  /** Buổi được đặt trước mốc nhắc (createdAt ≤ startsAt − mốc). */
  private static boolean bookedBefore(Session s, Duration mark) {
    return s.getCreatedAt() != null && !s.getCreatedAt().isAfter(s.getStartsAt().minus(mark));
  }

  private void publish(Session s, ReminderWindow window) {
    events.publishEvent(
        new SessionReminderDue(
            s.getId(), s.getTeacherId(), s.getStudentId(), s.getStartsAt(), window));
  }
}
