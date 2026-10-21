package com.englishplatform.notification.internal;

import com.englishplatform.identity.PasswordResetRequested;
import com.englishplatform.identity.UserInvited;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Email mời kích hoạt tài khoản và email đặt lại mật khẩu. */
@Component
class IdentityNotificationListener {

  private final DispatchService dispatchService;
  private final String frontendUrl;

  IdentityNotificationListener(
      DispatchService dispatchService, @Value("${app.frontend-url}") String frontendUrl) {
    this.dispatchService = dispatchService;
    this.frontendUrl = frontendUrl;
  }

  @ApplicationModuleListener
  void on(UserInvited event) {
    String link = frontendUrl + "/accept-invite?token=" + encode(event.inviteToken());
    dispatchService.notify(
        "user:" + event.userId() + ":invite:" + event.tokenId(),
        event.userId(),
        event.email(),
        "Mời kích hoạt tài khoản English 1-1",
        "invite",
        Map.of("fullName", event.fullName(), "link", link),
        null);
  }

  @ApplicationModuleListener
  void on(PasswordResetRequested event) {
    String link = frontendUrl + "/reset-password?token=" + encode(event.resetToken());
    dispatchService.notify(
        "user:" + event.userId() + ":reset:" + event.tokenId(),
        event.userId(),
        event.email(),
        "Đặt lại mật khẩu English 1-1",
        "reset-password",
        Map.of("link", link),
        null);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
