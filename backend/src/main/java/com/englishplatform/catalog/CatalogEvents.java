package com.englishplatform.catalog;

import java.time.Instant;
import java.util.UUID;

/** Event do catalog phát — payload đúng contracts/events-and-ports.md §1. */
public final class CatalogEvents {

  private CatalogEvents() {}

  public record EnrollmentCreated(
      UUID enrollmentId, UUID studentId, UUID teacherId, long priceVnd) {}

  public record EnrollmentActivated(UUID enrollmentId, UUID studentId, Instant expiresAt) {}

  public record TeacherReassigned(
      UUID enrollmentId, UUID studentId, UUID fromTeacherId, UUID toTeacherId) {}

  public enum RenewalReason {
    LOW_SESSIONS,
    EXPIRING
  }

  public record EnrollmentRenewalDue(
      UUID enrollmentId,
      UUID studentId,
      int sessionsRemaining,
      Instant expiresAt,
      RenewalReason reason) {}
}
