package com.englishplatform.scheduling;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Public API đọc buổi học cho learning (events-and-ports.md §2). Bản thật ở U3. */
public interface SessionQueryApi {

  Optional<SessionView> find(UUID sessionId);

  record SessionView(
      UUID id,
      UUID enrollmentId,
      UUID teacherId,
      UUID studentId,
      Instant startsAt,
      Instant endsAt,
      SessionStatus status) {}
}
