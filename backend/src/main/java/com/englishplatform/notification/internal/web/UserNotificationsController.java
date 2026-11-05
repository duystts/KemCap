package com.englishplatform.notification.internal.web;

import com.englishplatform.notification.internal.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Giáo vụ đọc thông báo của một người dùng để lập bản tóm tắt dữ liệu cá nhân (FR-031). */
@RestController
class UserNotificationsController {

  private final NotificationService notificationService;

  UserNotificationsController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/api/v1/users/{userId}/notifications")
  @PreAuthorize("hasRole('ADMIN')")
  List<NotificationDto> list(@PathVariable UUID userId) {
    return notificationService.listAll(userId).stream().map(NotificationDto::from).toList();
  }
}
