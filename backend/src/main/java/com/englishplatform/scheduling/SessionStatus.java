package com.englishplatform.scheduling;

/** Trạng thái buổi học (FR-014) — data-model.md module scheduling. */
public enum SessionStatus {
  BOOKED,
  COMPLETED,
  STUDENT_NO_SHOW,
  TEACHER_ABSENT,
  CANCELLED_ON_TIME,
  CANCELLED_LATE,
  CANCELLED_SYSTEM,
  INTERRUPTED
}
