package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.englishplatform.identity.UserDirectory;
import com.englishplatform.notification.EmailSender;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.thymeleaf.TemplateEngine;

/** Lỗi SMTP thường nhắc địa chỉ người nhận; log không được chứa email (NFR-PRIV-02, R3). */
@ExtendWith(OutputCaptureExtension.class)
class DispatchServiceLogTest {

  private static final Instant T0 = Instant.parse("2026-11-01T00:00:00Z");

  @Test
  void failedSendLogDoesNotContainRecipientEmail(CapturedOutput output) {
    EmailSender sender = mock(EmailSender.class);
    doThrow(new IllegalStateException("550 mailbox hv.lan@example.com not found"))
        .when(sender)
        .send(any());
    DispatchService service =
        new DispatchService(
            mock(NotificationDispatchRepository.class),
            mock(NotificationRepository.class),
            sender,
            mock(UserDirectory.class),
            mock(TemplateEngine.class),
            Clock.fixed(T0, ZoneOffset.UTC));

    NotificationDispatch dispatch =
        new NotificationDispatch("k1", null, "hv.lan@example.com", "s", "<p>x</p>", T0);
    service.attempt(dispatch);

    assertThat(output).contains("k1").doesNotContain("hv.lan@example.com");
    assertThat(dispatch.getStatus()).isEqualTo(NotificationDispatch.Status.PENDING);
  }
}
