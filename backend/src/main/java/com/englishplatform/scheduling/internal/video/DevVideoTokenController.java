package com.englishplatform.scheduling.internal.video;

import com.englishplatform.scheduling.VideoRoomProvider;
import com.englishplatform.shared.security.CurrentUser;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chỉ cho spike LiveKit trong U1 (T032), profile {@code local}: cấp token vào một room thử cho
 * người dùng đã đăng nhập. Xóa khi {@code POST /sessions/{id}/join} thật có ở U3.
 */
@RestController
@Profile("local")
@PreAuthorize("isAuthenticated()")
class DevVideoTokenController {

  private static final UUID SPIKE_ROOM = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final VideoRoomProvider videoRoomProvider;
  private final Clock clock;

  DevVideoTokenController(VideoRoomProvider videoRoomProvider, Clock clock) {
    this.videoRoomProvider = videoRoomProvider;
    this.clock = clock;
  }

  @GetMapping("/api/v1/dev/video-token")
  VideoRoomProvider.JoinInfo token(@RequestParam(defaultValue = "STUDENT") String role) {
    CurrentUser user = CurrentUser.require();
    return videoRoomProvider.issueJoin(
        SPIKE_ROOM,
        user.id(),
        user.name(),
        VideoRoomProvider.ParticipantRole.valueOf(role),
        clock.instant().plus(Duration.ofHours(1)));
  }
}
