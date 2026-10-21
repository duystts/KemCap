package com.englishplatform.notification.internal;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mỗi phút gửi lại email PENDING đã tới {@code next_attempt_at} (NFR-REL-06). */
@Component
class RetryDispatchJob {

  private static final Logger log = LoggerFactory.getLogger(RetryDispatchJob.class);

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
  public void run() {
    List<UUID> due =
        dispatches
            .findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
                NotificationDispatch.Status.PENDING, clock.instant(), Limit.of(50))
            .stream()
            .map(NotificationDispatch::getId)
            .toList();
    for (UUID id : due) {
      try {
        dispatchService.retry(id);
      } catch (OptimisticLockingFailureException e) {
        // bản ghi vừa bị ẩn danh hóa (FR-031) — bỏ qua, lượt sau không còn PENDING
        log.info("Bỏ qua email {} vì vừa thay đổi song song", id);
      }
    }
  }
}
