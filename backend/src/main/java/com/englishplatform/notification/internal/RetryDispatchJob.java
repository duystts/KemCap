package com.englishplatform.notification.internal;

import java.time.Clock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Mỗi phút gửi lại email PENDING đã tới {@code next_attempt_at} (NFR-REL-06). */
@Component
class RetryDispatchJob {

  private final NotificationDispatchRepository dispatches;
  private final DispatchService dispatchService;
  private final Clock clock;

  RetryDispatchJob(
      NotificationDispatchRepository dispatches, DispatchService dispatchService, Clock clock) {
    this.dispatches = dispatches;
    this.dispatchService = dispatchService;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "PT60S", initialDelayString = "${app.jobs.initial-delay}")
  @SchedulerLock(name = "notification.retryDispatch", lockAtMostFor = "PT5M")
  @Transactional
  public void run() {
    dispatches
        .findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
            NotificationDispatch.Status.PENDING, clock.instant(), Limit.of(50))
        .forEach(dispatchService::attempt);
  }
}
