package com.englishplatform.learning;

import java.time.Instant;
import java.util.UUID;

/** Event do learning phát — payload đúng contracts/events-and-ports.md §1. */
public final class LearningEvents {

  private LearningEvents() {}

  public record SessionReportOverdue(UUID sessionId, UUID teacherId, Instant dueAt) {}

  public record SessionReportSubmitted(UUID sessionId, UUID studentId) {}

  public record AssignmentSubmitted(UUID assignmentId, UUID studentId, UUID teacherId) {}

  public record AssignmentGraded(UUID assignmentId, UUID studentId, int score) {}
}
