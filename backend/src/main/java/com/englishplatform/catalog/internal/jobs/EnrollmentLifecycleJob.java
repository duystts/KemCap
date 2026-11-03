package com.englishplatform.catalog.internal.jobs;

import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hằng ngày 00:05 giờ VN (FR-024, FR-026): hết hạn gói và nhắc gia hạn. Mỗi ghi danh một
 * transaction: một bản đang được thao tác (xung đột version) chỉ lùi sang lần chạy sau, không làm
 * hỏng các bản khác.
 */
@Component
public class EnrollmentLifecycleJob {

  private static final Logger log = LoggerFactory.getLogger(EnrollmentLifecycleJob.class);

  private final EnrollmentRepository enrollments;
  private final EnrollmentLifecycleStep step;
  private final Clock clock;

  EnrollmentLifecycleJob(
      EnrollmentRepository enrollments, EnrollmentLifecycleStep step, Clock clock) {
    this.enrollments = enrollments;
    this.step = step;
    this.clock = clock;
  }

  @Scheduled(cron = "${app.jobs.enrollment-lifecycle-cron:0 5 0 * * *}", zone = "Asia/Ho_Chi_Minh")
  @SchedulerLock(name = "catalog.enrollmentLifecycle", lockAtMostFor = "PT30M")
  public void run() {
    Instant now = clock.instant();
    for (UUID id :
        enrollments.findIdsByStatusIn(
            EnumSet.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.EXHAUSTED))) {
      try {
        step.process(id, now);
      } catch (ObjectOptimisticLockingFailureException e) {
        log.warn("Ghi danh {} đang được thao tác, xử lý ở lần chạy sau", id);
      } catch (RuntimeException e) {
        log.error("Không xử lý được ghi danh {}, xử lý ở lần chạy sau", id, e);
      }
    }
  }
}
