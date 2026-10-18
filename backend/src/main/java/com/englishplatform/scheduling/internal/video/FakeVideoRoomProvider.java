package com.englishplatform.scheduling.internal.video;

import com.englishplatform.scheduling.VideoRoomProvider;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adapter giả cho test và chạy local không cần LiveKit. */
@Component
@ConditionalOnProperty(name = "app.providers.video", havingValue = "fake")
class FakeVideoRoomProvider implements VideoRoomProvider {

  @Override
  public JoinInfo issueJoin(
      UUID sessionId, UUID userId, String displayName, ParticipantRole role, Instant validUntil) {
    return new JoinInfo("ws://fake-video.local", "fake-token-" + userId, "session-" + sessionId);
  }
}
