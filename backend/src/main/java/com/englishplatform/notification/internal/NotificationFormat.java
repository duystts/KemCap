package com.englishplatform.notification.internal;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Định dạng thời gian theo múi giờ của người nhận (mặc định {@code Asia/Ho_Chi_Minh}). */
final class NotificationFormat {

  static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private NotificationFormat() {}

  static String dateTime(Instant instant, String timezone) {
    return instant == null ? "" : instant.atZone(zone(timezone)).format(DATE_TIME);
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
