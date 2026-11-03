package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.Role;
import com.englishplatform.notification.EmailSender.EmailMessage;
import com.englishplatform.support.ApiTestSupport;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * NFR-REL-04: listener lỗi không mất event; publication còn lại và được xử lý khi phát lại, như
 * {@code republish-outstanding-events-on-restart} làm lúc khởi động. F1 từ verify U5.
 */
class EventRedeliveryIT extends ApiTestSupport {

  @MockitoSpyBean DispatchService dispatchService;
  @Autowired IncompleteEventPublications incomplete;
  @Autowired FakeEmailSender emailSender;
  @Autowired JdbcTemplate jdbc;

  private long publicationsFor(String email) {
    return jdbc.queryForObject(
        "select count(*) from event_publication where serialized_event like ?",
        Long.class,
        "%" + email + "%");
  }

  @Test
  void failedListenerIsProcessedOnRedelivery() throws Exception {
    TestUser admin = user(Role.ADMIN);
    String email = uniqueEmail();
    doThrow(new IllegalStateException("listener sập"))
        .doCallRealMethod()
        .when(dispatchService)
        .notify(any(), any(), any(), any(), any(), any(), any());

    call(
            post("/api/v1/users"),
            admin,
            "{\"email\":\"" + email + "\",\"fullName\":\"Giáo viên\",\"role\":\"TEACHER\"}")
        .andExpect(status().isCreated());

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(publicationsFor(email)).isEqualTo(1);
              org.mockito.Mockito.verify(dispatchService)
                  .notify(any(), any(), any(), any(), any(), any(), any());
            });
    assertThat(emailSender.sent()).extracting(EmailMessage::to).doesNotContain(email);

    doCallRealMethod()
        .when(dispatchService)
        .notify(any(), any(), any(), any(), any(), any(), any());
    incomplete.resubmitIncompletePublications(p -> true);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(emailSender.sent()).extracting(EmailMessage::to).contains(email);
              assertThat(publicationsFor(email)).isZero();
            });
  }
}
