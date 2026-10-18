package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Backoff 1 → 5 → 15 phút sau lần gửi đầu, rồi {@code FAILED} (NFR-REL-06). */
class NotificationDispatchTest {

  static final Instant T0 = Instant.parse("2026-11-01T00:00:00Z");

  @Test
  void backsOffThenFailsAfterThreeRetries() {
    NotificationDispatch d = new NotificationDispatch("k", "a@test.local", "s", "<p>x</p>", T0);

    d.markFailed(T0, "lỗi 1");
    assertThat(d.getStatus()).isEqualTo(NotificationDispatch.Status.PENDING);
    assertThat(d.getNextAttemptAt()).isEqualTo(T0.plus(Duration.ofMinutes(1)));

    d.markFailed(T0, "lỗi 2");
    assertThat(d.getNextAttemptAt()).isEqualTo(T0.plus(Duration.ofMinutes(5)));

    d.markFailed(T0, "lỗi 3");
    assertThat(d.getNextAttemptAt()).isEqualTo(T0.plus(Duration.ofMinutes(15)));
    assertThat(d.getBodyHtml()).isEqualTo("<p>x</p>");

    d.markFailed(T0, "lỗi 4");
    assertThat(d.getStatus()).isEqualTo(NotificationDispatch.Status.FAILED);
    assertThat(d.getAttempts()).isEqualTo((short) 4);
    assertThat(d.getNextAttemptAt()).isNull();
    assertThat(d.getError()).isEqualTo("lỗi 4");
    assertThat(d.getBodyHtml()).isEqualTo(NotificationDispatch.SCRUBBED_BODY);
  }

  @Test
  void sentClearsBodyAndSchedule() {
    NotificationDispatch d = new NotificationDispatch("k", "a@test.local", "s", "<p>token</p>", T0);
    d.markFailed(T0, "lỗi");
    d.markSent(T0.plus(Duration.ofMinutes(1)));

    assertThat(d.getStatus()).isEqualTo(NotificationDispatch.Status.SENT);
    assertThat(d.getAttempts()).isEqualTo((short) 2);
    assertThat(d.getNextAttemptAt()).isNull();
    assertThat(d.getError()).isNull();
    assertThat(d.getBodyHtml()).isEqualTo(NotificationDispatch.SCRUBBED_BODY);
  }
}
