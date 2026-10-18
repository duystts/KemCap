package com.englishplatform.scheduling.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.scheduling.*} — chỉ đặt qua cấu hình triển khai, không có API/màn hình (quyết định
 * 2026-10-18). Ví dụ {@code APP_SCHEDULING_CANCEL_CUTOFF_HOURS=24}.
 */
@ConfigurationProperties("app.scheduling")
public record SchedulingProperties(
    int cancelCutoffHours,
    int minBookingLeadHours,
    int bookingHorizonDays,
    int roomOpenBeforeMinutes,
    int roomCloseAfterMinutes) {

  /** Bước giữa hai slot liên tiếp (research R4). */
  public static final Duration SLOT_STEP = Duration.ofMinutes(30);

  public Duration cancelCutoff() {
    return Duration.ofHours(cancelCutoffHours);
  }

  public Duration minBookingLead() {
    return Duration.ofHours(minBookingLeadHours);
  }

  public Duration bookingHorizon() {
    return Duration.ofDays(bookingHorizonDays);
  }

  public Duration roomOpenBefore() {
    return Duration.ofMinutes(roomOpenBeforeMinutes);
  }

  public Duration roomCloseAfter() {
    return Duration.ofMinutes(roomCloseAfterMinutes);
  }
}
