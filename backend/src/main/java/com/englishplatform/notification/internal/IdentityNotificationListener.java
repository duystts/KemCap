package com.englishplatform.notification.internal;

import com.englishplatform.identity.PasswordResetRequested;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserInvited;
import com.englishplatform.identity.UserView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Email mời kích hoạt tài khoản và email đặt lại mật khẩu. Đọc lại người nhận thay vì tin email/tên
 * trong event: event có thể tới sau lúc ẩn danh hóa, khi địa chỉ cũ không còn được dùng (FR-031).
 */
@Component
class IdentityNotificationListener {

  private final DispatchService dispatchService;
  private final UserDirectory userDirectory;
  private final String frontendUrl;

  IdentityNotificationListener(
      DispatchService dispatchService,
      UserDirectory userDirectory,
      @Value("${app.frontend-url}") String frontendUrl) {
    this.dispatchService = dispatchService;
    this.userDirectory = userDirectory;
    this.frontendUrl = frontendUrl;
  }

  @ApplicationModuleListener
  void on(UserInvited event) {
    UserView user = userDirectory.find(event.userId()).orElse(null);
    if (user == null) {
      return;
    }
    String link = frontendUrl + "/accept-invite?token=" + encode(event.inviteToken());
    dispatchService.notify(
        "user:" + event.userId() + ":invite:" + event.tokenId(),
        event.userId(),
        user.email(),
        "Mời kích hoạt tài khoản English 1-1",
        "invite",
        Map.of("fullName", user.fullName(), "link", link),
        null);
  }

  @ApplicationModuleListener
  void on(PasswordResetRequested event) {
    UserView user = userDirectory.find(event.userId()).orElse(null);
    if (user == null) {
      return;
    }
    String link = frontendUrl + "/reset-password?token=" + encode(event.resetToken());
    dispatchService.notify(
        "user:" + event.userId() + ":reset:" + event.tokenId(),
        event.userId(),
        user.email(),
        "Đặt lại mật khẩu English 1-1",
        "reset-password",
        Map.of("link", link),
        null);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
