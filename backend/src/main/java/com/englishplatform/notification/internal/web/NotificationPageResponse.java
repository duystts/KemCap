package com.englishplatform.notification.internal.web;

import java.util.List;

public record NotificationPageResponse(
    List<NotificationDto> items,
    long totalElements,
    int totalPages,
    int page,
    int size,
    long unreadCount) {}
