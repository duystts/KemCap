package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;

class NotificationServiceTest {

  private NotificationRepository repository;
  private Clock clock;
  private NotificationService service;

  private final UUID userId = UUID.randomUUID();
  private final UUID notifId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-01T10:00:00Z");

  @BeforeEach
  void setUp() {
    repository = mock(NotificationRepository.class);
    clock = Clock.fixed(now, ZoneOffset.UTC);
    service = new NotificationService(repository, clock);
  }

  @Test
  void list_queriesRepository() {
    PageRequest pageable = PageRequest.of(0, 20);
    Notification n = new Notification(userId, "TYPE", "Title", "Body", "/link");
    when(repository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
        .thenReturn(new PageImpl<>(List.of(n)));

    Page<Notification> result = service.list(userId, false, pageable);
    assertThat(result.getContent()).hasSize(1);
    verify(repository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
  }

  @Test
  void markRead_whenFound_marksRead() {
    Notification n = new Notification(userId, "TYPE", "Title", "Body", "/link");
    when(repository.findByIdAndUserId(notifId, userId)).thenReturn(Optional.of(n));

    service.markRead(userId, notifId);
    assertThat(n.getReadAt()).isEqualTo(now);
  }

  @Test
  void markRead_whenNotFound_throwsNotFound() {
    when(repository.findByIdAndUserId(notifId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.markRead(userId, notifId))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", ErrorCode.NOT_FOUND);
  }

  @Test
  void markAllRead_callsRepositoryWithNow() {
    service.markAllRead(userId);
    verify(repository).markAllRead(userId, now);
  }
}
