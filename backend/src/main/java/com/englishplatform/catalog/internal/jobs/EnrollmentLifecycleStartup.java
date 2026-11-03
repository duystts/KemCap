package com.englishplatform.catalog.internal.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Chạy bù khi khởi động: demo chạy trên laptop nên máy có thể tắt đúng lúc 00:05. Job chạy lặp an
 * toàn (cờ đã nhắc, ShedLock); lỗi ở đây chỉ ghi log, không chặn app khởi động.
 */
@Component
@ConditionalOnProperty(
    name = "app.jobs.enrollment-lifecycle-on-startup",
    havingValue = "true",
    matchIfMissing = true)
class EnrollmentLifecycleStartup {

  private static final Logger log = LoggerFactory.getLogger(EnrollmentLifecycleStartup.class);

  private final EnrollmentLifecycleJob job;

  EnrollmentLifecycleStartup(EnrollmentLifecycleJob job) {
    this.job = job;
  }

  @EventListener(ApplicationReadyEvent.class)
  void catchUp() {
    try {
      job.run();
    } catch (RuntimeException e) {
      log.error("Chạy bù job gia hạn khi khởi động thất bại", e);
    }
  }
}
