package com.englishplatform.notification.internal;

import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationRepository notifications;
  private final Clock clock;

  public NotificationService(NotificationRepository notifications, Clock clock) {
    this.notifications = notifications;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Page<Notification> list(UUID userId, boolean unreadOnly, Pageable pageable) {
    if (unreadOnly) {
      return notifications.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable);
    }
    return notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
  }

  @Transactional(readOnly = true)
  public long countUnread(UUID userId) {
    return notifications.countByUserIdAndReadAtIsNull(userId);
  }

  @Transactional
  public void markRead(UUID userId, UUID notificationId) {
    Notification notification =
        notifications
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    notification.markRead(clock.instant());
  }

  @Transactional
  public int markAllRead(UUID userId) {
    return notifications.markAllRead(userId, clock.instant());
  }
}
