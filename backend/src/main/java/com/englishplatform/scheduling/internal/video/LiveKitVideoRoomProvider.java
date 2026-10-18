package com.englishplatform.scheduling.internal.video;

import com.englishplatform.scheduling.VideoRoomProvider;
import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanPublishData;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter LiveKit: token chỉ cho room {@code session-{id}}, identity = userId, hết hạn đúng lúc
 * đóng cửa sổ vào phòng. SDK chỉ được import trong package adapter này (constitution II).
 */
@Component
@ConditionalOnProperty(name = "app.providers.video", havingValue = "livekit", matchIfMissing = true)
class LiveKitVideoRoomProvider implements VideoRoomProvider {

  private final String serverUrl;
  private final String apiKey;
  private final String apiSecret;
  private final Clock clock;

  LiveKitVideoRoomProvider(
      @Value("${app.livekit.url}") String serverUrl,
      @Value("${app.livekit.api-key}") String apiKey,
      @Value("${app.livekit.api-secret}") String apiSecret,
      Clock clock) {
    this.serverUrl = serverUrl;
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.clock = clock;
  }

  @Override
  public JoinInfo issueJoin(
      UUID sessionId, UUID userId, String displayName, ParticipantRole role, Instant validUntil) {
    String roomName = "session-" + sessionId;
    AccessToken token = new AccessToken(apiKey, apiSecret);
    token.setIdentity(userId.toString());
    token.setName(displayName);
    token.setMetadata(role.name());
    long ttlMillis = Duration.between(clock.instant(), validUntil).toMillis();
    if (ttlMillis <= 0) {
      // không cấp token đã hết hạn / kéo dài quá cửa sổ phòng (NFR-SEC-08)
      throw new IllegalArgumentException("validUntil phải ở tương lai");
    }
    token.setTtl(ttlMillis);
    token.addGrants(
        new RoomJoin(true),
        new RoomName(roomName),
        new CanPublish(true),
        new CanSubscribe(true),
        new CanPublishData(true));
    return new JoinInfo(serverUrl, token.toJwt(), roomName);
  }
}
