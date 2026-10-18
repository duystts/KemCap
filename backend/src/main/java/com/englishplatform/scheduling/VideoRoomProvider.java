package com.englishplatform.scheduling;

import java.time.Instant;
import java.util.UUID;

/**
 * Port phòng học video (constitution II). Adapter chọn bằng {@code app.providers.video}: {@code
 * livekit} hoặc {@code fake}.
 */
public interface VideoRoomProvider {

  JoinInfo issueJoin(
      UUID sessionId, UUID userId, String displayName, ParticipantRole role, Instant validUntil);

  enum ParticipantRole {
    TEACHER,
    STUDENT
  }

  record JoinInfo(String serverUrl, String token, String roomName) {}
}
