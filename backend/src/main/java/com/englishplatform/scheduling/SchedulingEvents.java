package com.englishplatform.scheduling;

import java.time.Instant;
import java.util.UUID;

/** Event do scheduling phát — payload đúng contracts/events-and-ports.md §1. */
public final class SchedulingEvents {

  private SchedulingEvents() {}

  public record SessionBooked(
      UUID sessionId,
      UUID enrollmentId,
      UUID teacherId,
      UUID studentId,
      Instant startsAt,
      Instant endsAt) {}

  public record SessionCancelled(
      UUID sessionId,
      UUID enrollmentId,
      SessionStatus status,
      UUID cancelledBy,
      Instant startsAt) {}

  public record SessionRescheduled(UUID oldSessionId, UUID newSessionId, Instant newStartsAt) {}

  public enum ReminderWindow {
    H24,
    H1
  }

  public record SessionReminderDue(
      UUID sessionId, UUID teacherId, UUID studentId, Instant startsAt, ReminderWindow window) {}

  public record SessionFinished(
      UUID sessionId,
      UUID enrollmentId,
      UUID teacherId,
      UUID studentId,
      SessionStatus outcome,
      Instant endsAt) {}

  public record SessionOutcomeOverdue(UUID sessionId, UUID teacherId, Instant endsAt) {}
}
