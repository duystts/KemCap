-- Module scheduling: lịch rảnh hằng tuần, ngày nghỉ, buổi học (chống trùng bằng exclusion constraint)

-- giờ theo múi giờ của giáo viên (user_account.timezone)
CREATE TABLE availability_rule
(
  id          UUID        NOT NULL PRIMARY KEY,
  teacher_id  UUID        NOT NULL,
  day_of_week SMALLINT    NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
  start_time  TIME        NOT NULL,
  end_time    TIME        NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL,
  updated_at  TIMESTAMPTZ NOT NULL,
  CONSTRAINT availability_rule_time_chk CHECK (end_time > start_time)
);
CREATE INDEX availability_rule_teacher_idx ON availability_rule (teacher_id, day_of_week);

-- start_time/end_time cùng null = nghỉ cả ngày
CREATE TABLE availability_exception
(
  id         UUID         NOT NULL PRIMARY KEY,
  teacher_id UUID         NOT NULL,
  date       DATE         NOT NULL,
  start_time TIME,
  end_time   TIME,
  reason     VARCHAR(200),
  created_at TIMESTAMPTZ  NOT NULL,
  updated_at TIMESTAMPTZ  NOT NULL,
  CONSTRAINT availability_exception_time_chk CHECK (
    (start_time IS NULL AND end_time IS NULL)
      OR (start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time))
);
CREATE INDEX availability_exception_teacher_idx ON availability_exception (teacher_id, date);

CREATE TABLE session
(
  id                          UUID        NOT NULL PRIMARY KEY,
  enrollment_id               UUID        NOT NULL,
  teacher_id                  UUID        NOT NULL,
  student_id                  UUID        NOT NULL,
  starts_at                   TIMESTAMPTZ NOT NULL,
  ends_at                     TIMESTAMPTZ NOT NULL,
  status                      VARCHAR(20) NOT NULL CHECK (status IN (
    'BOOKED', 'COMPLETED', 'STUDENT_NO_SHOW', 'TEACHER_ABSENT',
    'CANCELLED_ON_TIME', 'CANCELLED_LATE', 'CANCELLED_SYSTEM', 'INTERRUPTED')),
  booked_by                   UUID        NOT NULL,
  cancelled_by                UUID,
  cancel_reason               TEXT,
  rescheduled_from_id         UUID REFERENCES session (id),
  teacher_joined_at           TIMESTAMPTZ,
  student_joined_at           TIMESTAMPTZ,
  reminder_24h_at             TIMESTAMPTZ,
  reminder_1h_at              TIMESTAMPTZ,
  outcome_overdue_notified_at TIMESTAMPTZ,
  outcome_by                  UUID,
  outcome_at                  TIMESTAMPTZ,
  version                     BIGINT      NOT NULL DEFAULT 0,
  created_at                  TIMESTAMPTZ NOT NULL,
  updated_at                  TIMESTAMPTZ NOT NULL,
  CONSTRAINT session_time_chk CHECK (ends_at > starts_at),
  -- 0 trùng lịch (NFR-REL-01, R3): một giáo viên / một học viên không có hai buổi chồng giờ
  CONSTRAINT session_teacher_no_overlap EXCLUDE USING gist (
    teacher_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
    WHERE (status IN ('BOOKED', 'COMPLETED', 'STUDENT_NO_SHOW', 'CANCELLED_LATE', 'INTERRUPTED')),
  CONSTRAINT session_student_no_overlap EXCLUDE USING gist (
    student_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
    WHERE (status IN ('BOOKED', 'COMPLETED', 'STUDENT_NO_SHOW', 'CANCELLED_LATE', 'INTERRUPTED'))
);
CREATE INDEX session_teacher_starts_idx ON session (teacher_id, starts_at);
CREATE INDEX session_student_starts_idx ON session (student_id, starts_at);
CREATE INDEX session_status_starts_idx ON session (status, starts_at);
CREATE INDEX session_enrollment_idx ON session (enrollment_id);
