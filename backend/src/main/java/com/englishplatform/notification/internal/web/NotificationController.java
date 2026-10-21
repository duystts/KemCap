package com.englishplatform.notification.internal.web;

import com.englishplatform.notification.internal.Notification;
import com.englishplatform.notification.internal.NotificationService;
import com.englishplatform.shared.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API thông báo trong hệ thống (T135, rest-api.md) — chỉ thông báo của chính người dùng. */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public NotificationPageResponse list(
      @RequestParam(name = "unread", required = false, defaultValue = "false") boolean unread,
      @RequestParam(name = "page", required = false, defaultValue = "0") int page,
      @RequestParam(name = "size", required = false, defaultValue = "20") int size) {
    CurrentUser user = CurrentUser.require();
    int sanitizedSize = Math.min(Math.max(1, size), 50);
    int sanitizedPage = Math.max(0, page);
    Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize);

    Page<Notification> paged = notificationService.list(user.id(), unread, pageable);
    long unreadCount = notificationService.countUnread(user.id());

    List<NotificationDto> dtos = paged.getContent().stream().map(NotificationDto::from).toList();

    return new NotificationPageResponse(
        dtos,
        paged.getTotalElements(),
        paged.getTotalPages(),
        paged.getNumber(),
        paged.getSize(),
        unreadCount);
  }

  /** Thông báo của người khác → 404 (không lộ sự tồn tại). */
  @PostMapping("/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markRead(@PathVariable("id") UUID id) {
    notificationService.markRead(CurrentUser.require().id(), id);
  }

  @PostMapping("/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markAllRead() {
    notificationService.markAllRead(CurrentUser.require().id());
  }
}
