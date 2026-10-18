package com.englishplatform.catalog;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentView(
    UUID id,
    UUID studentId,
    UUID teacherId,
    int sessionsTotal,
    int sessionsReserved,
    int sessionsConsumed,
    int sessionMinutes,
    Instant expiresAt,
    EnrollmentStatus status) {

  public int sessionsRemaining() {
    return sessionsTotal - sessionsReserved - sessionsConsumed;
  }
}
