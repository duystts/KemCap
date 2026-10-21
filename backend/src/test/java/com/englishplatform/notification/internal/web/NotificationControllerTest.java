package com.englishplatform.notification.internal.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.notification.internal.Notification;
import com.englishplatform.notification.internal.NotificationService;
import com.englishplatform.shared.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationControllerTest {

  private NotificationService service;
  private MockMvc mvc;
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = mock(NotificationService.class);
    NotificationController controller = new NotificationController(service);
    mvc = MockMvcBuilders.standaloneSetup(controller).build();

    Jwt jwt =
        Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .subject(userId.toString())
            .claim(CurrentUser.CLAIM_ROLE, "STUDENT")
            .claim(CurrentUser.CLAIM_NAME, "Test User")
            .claim(CurrentUser.CLAIM_TIMEZONE, "Asia/Ho_Chi_Minh")
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void list_returnsPagedNotificationsAndUnreadCount() throws Exception {
    Notification n = new Notification(userId, "TYPE", "Tiêu đề", "Nội dung", "/link");
    when(service.list(eq(userId), eq(false), any()))
        .thenReturn(new PageImpl<>(List.of(n), PageRequest.of(0, 20), 1));
    when(service.countUnread(userId)).thenReturn(1L);

    mvc.perform(get("/api/v1/notifications?unread=false&page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.unreadCount").value(1))
        .andExpect(jsonPath("$.items[0].title").value("Tiêu đề"))
        .andExpect(jsonPath("$.items[0].link").value("/link"));

    verify(service).list(eq(userId), eq(false), any());
    verify(service).countUnread(userId);
  }

  @Test
  void markRead_callsService() throws Exception {
    UUID notifId = UUID.randomUUID();

    mvc.perform(post("/api/v1/notifications/" + notifId + "/read"))
        .andExpect(status().isNoContent());

    verify(service).markRead(userId, notifId);
  }

  @Test
  void markAllRead_callsService() throws Exception {
    mvc.perform(post("/api/v1/notifications/read-all")).andExpect(status().isNoContent());

    verify(service).markAllRead(userId);
  }
}
