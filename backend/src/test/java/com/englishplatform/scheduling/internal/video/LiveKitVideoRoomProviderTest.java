package com.englishplatform.scheduling.internal.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.englishplatform.scheduling.VideoRoomProvider.JoinInfo;
import com.englishplatform.scheduling.VideoRoomProvider.ParticipantRole;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Adapter {@code livekit} (không cần server): token đúng room, identity, quyền và hạn. */
class LiveKitVideoRoomProviderTest {

  static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

  LiveKitVideoRoomProvider provider =
      new LiveKitVideoRoomProvider(
          "wss://demo.livekit.cloud",
          "devkey",
          "secret-0123456789",
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void tokenIsScopedToSessionRoomAndExpiresWhenRoomCloses() {
    UUID session = UUID.randomUUID();
    UUID user = UUID.randomUUID();
    JoinInfo info =
        provider.issueJoin(
            session, user, "Nguyễn Lan", ParticipantRole.TEACHER, NOW.plus(Duration.ofMinutes(70)));
    assertThat(info.roomName()).isEqualTo("session-" + session);
    assertThat(info.serverUrl()).isEqualTo("wss://demo.livekit.cloud");
    String claims =
        new String(
            Base64.getUrlDecoder().decode(info.token().split("\\.")[1]), StandardCharsets.UTF_8);
    assertThat(claims)
        .contains("\"sub\":\"" + user + "\"")
        .contains("\"room\":\"session-" + session + "\"")
        .contains("\"roomJoin\":true")
        .contains("\"canPublish\":true");
    long exp = Long.parseLong(claims.replaceAll(".*\"exp\":(\\d+).*", "$1"));
    // SDK cộng TTL vào giờ hệ thống; TTL = validUntil − clock của app (ở production là một)
    Instant realNow = Instant.now();
    assertThat(Instant.ofEpochSecond(exp))
        .isBetween(realNow.plus(Duration.ofMinutes(69)), realNow.plus(Duration.ofMinutes(71)));
  }

  @Test
  void refusesExpiredValidity() {
    assertThatThrownBy(
            () ->
                provider.issueJoin(
                    UUID.randomUUID(), UUID.randomUUID(), "A", ParticipantRole.STUDENT, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
