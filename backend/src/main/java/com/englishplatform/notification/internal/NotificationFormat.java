package com.englishplatform.notification.internal;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Định dạng thời gian theo múi giờ của người nhận (mặc định {@code Asia/Ho_Chi_Minh}). */
final class NotificationFormat {

  static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private NotificationFormat() {}

  /**
   * Ví dụ {@code 18:00, 01/11/2026 (GMT+7)}. Ghi múi giờ vì nội dung được lưu lúc tạo, người nhận
   * có thể đọc ở múi giờ khác hoặc đã đổi múi giờ sau đó (T150, U5 R7).
   */
  static String dateTime(Instant instant, String timezone) {
    if (instant == null) {
      return "";
    }
    ZonedDateTime at = instant.atZone(zone(timezone));
    return at.format(DATE_TIME) + " (" + gmt(at.getOffset()) + ")";
  }

  /** {@code GMT+7}, {@code GMT-5}, {@code GMT+5:30}, {@code GMT+0}. */
  private static String gmt(ZoneOffset offset) {
    int seconds = offset.getTotalSeconds();
    int abs = Math.abs(seconds);
    int hours = abs / 3600;
    int minutes = abs % 3600 / 60;
    return "GMT"
        + (seconds < 0 ? "-" : "+")
        + hours
        + (minutes == 0 ? "" : ":" + String.format("%02d", minutes));
  }

  static String date(Instant instant, String timezone) {
    return instant == null ? "" : instant.atZone(zone(timezone)).format(DATE);
  }

  static ZoneId zone(String timezone) {
    if (timezone == null || timezone.isBlank()) {
      return DEFAULT_ZONE;
    }
    try {
      return ZoneId.of(timezone);
    } catch (DateTimeException e) {
      return DEFAULT_ZONE;
    }
  }
}
