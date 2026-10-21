package com.englishplatform.notification.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.Role;
import com.englishplatform.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationApiIT extends ApiTestSupport {

  @Autowired NotificationRepository notificationRepository;

  @Test
  void list_returnsOnlyCurrentUserNotifications() throws Exception {
    TestUser studentA = user(Role.STUDENT);
    TestUser studentB = user(Role.STUDENT);

    notificationRepository.save(
        new Notification(
            studentA.id(), "SESSION_BOOKED", "Thông báo cho A", "Nội dung A", "/student/schedule"));

    notificationRepository.save(
        new Notification(
            studentB.id(), "SESSION_BOOKED", "Thông báo cho B", "Nội dung B", "/student/schedule"));

    // Student A lấy danh sách thông báo
    call(get("/api/v1/notifications"), studentA, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.unreadCount").value(1))
        .andExpect(jsonPath("$.items[0].title").value("Thông báo cho A"));
  }

  @Test
  void markRead_marksOnlyUserOwnNotification() throws Exception {
    TestUser studentA = user(Role.STUDENT);
    TestUser studentB = user(Role.STUDENT);

    Notification notifA =
        notificationRepository.save(
            new Notification(
                studentA.id(),
                "SESSION_REMINDER",
                "Nhắc lịch A",
                "Buổi học sắp tới",
                "/student/schedule"));

    Notification notifB =
        notificationRepository.save(
            new Notification(
                studentB.id(),
                "SESSION_REMINDER",
                "Nhắc lịch B",
                "Buổi học sắp tới",
                "/student/schedule"));

    // Student A đánh dấu đọc thông báo của chính mình -> OK
    call(post("/api/v1/notifications/" + notifA.getId() + "/read"), studentA, null)
        .andExpect(status().isNoContent());

    assertThat(notificationRepository.findById(notifA.getId()).orElseThrow().getReadAt())
        .isNotNull();

    // Student A cố đánh dấu đọc thông báo của Student B -> 404 NOT_FOUND
    call(post("/api/v1/notifications/" + notifB.getId() + "/read"), studentA, null)
        .andExpect(status().isNotFound());
  }

  @Test
  void markAllRead_marksAllCurrentUserNotifications() throws Exception {
    TestUser student = user(Role.STUDENT);

    notificationRepository.save(
        new Notification(student.id(), "TYPE_1", "Tiêu đề 1", "Nội dung 1", "/link1"));
    notificationRepository.save(
        new Notification(student.id(), "TYPE_2", "Tiêu đề 2", "Nội dung 2", "/link2"));

    // Đánh dấu đọc tất cả
    call(post("/api/v1/notifications/read-all"), student, null).andExpect(status().isNoContent());

    // Kiểm tra unreadCount = 0
    call(get("/api/v1/notifications?unread=true"), student, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(0))
        .andExpect(jsonPath("$.totalElements").value(0));
  }
}
