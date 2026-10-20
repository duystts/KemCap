package com.englishplatform.notification.internal.web;

import com.englishplatform.notification.internal.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    String type,
    String title,
    String body,
    String link,
    Instant readAt,
    Instant createdAt) {

  public static NotificationDto from(Notification n) {
    return new NotificationDto(
        n.getId(),
        n.getType(),
        n.getTitle(),
        n.getBody(),
        n.getLink(),
        n.getReadAt(),
        n.getCreatedAt());
  }
}
