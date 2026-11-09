package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** T150 (U5 R7): giờ trong email và thông báo trong hệ thống ghi rõ múi giờ của người nhận. */
class NotificationFormatTest {

  static final Instant AT = Instant.parse("2026-11-01T11:00:00Z");

  @Test
  void dateTimeShowsOffsetOfRecipientZone() {
    assertThat(NotificationFormat.dateTime(AT, "Asia/Ho_Chi_Minh"))
        .isEqualTo("18:00, 01/11/2026 (GMT+7)");
    assertThat(NotificationFormat.dateTime(AT, "Europe/London"))
        .isEqualTo("11:00, 01/11/2026 (GMT+0)");
    assertThat(NotificationFormat.dateTime(AT, "America/New_York"))
        .isEqualTo("06:00, 01/11/2026 (GMT-5)");
    assertThat(NotificationFormat.dateTime(AT, "Asia/Kolkata"))
        .isEqualTo("16:30, 01/11/2026 (GMT+5:30)");
  }

  @Test
  void offsetFollowsDaylightSavingAtThatInstant() {
    assertThat(NotificationFormat.dateTime(Instant.parse("2026-07-01T11:00:00Z"), "Europe/London"))
        .isEqualTo("12:00, 01/07/2026 (GMT+1)");
  }

  @Test
  void missingOrInvalidZoneFallsBackToVietnam() {
    assertThat(NotificationFormat.dateTime(AT, null)).isEqualTo("18:00, 01/11/2026 (GMT+7)");
    assertThat(NotificationFormat.dateTime(AT, "Mars/Base")).isEqualTo("18:00, 01/11/2026 (GMT+7)");
    assertThat(NotificationFormat.dateTime(null, "Asia/Ho_Chi_Minh")).isEmpty();
  }

  @Test
  void dateOnlyHasNoOffset() {
    assertThat(NotificationFormat.date(AT, "Asia/Ho_Chi_Minh")).isEqualTo("01/11/2026");
  }
}
