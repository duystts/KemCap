-- Module catalog: hồ sơ giáo viên/học viên, khóa học, gói học, ghi danh, lịch sử phân công

CREATE TABLE teacher_profile
(
  user_id      UUID        NOT NULL PRIMARY KEY,
  bio          TEXT,
  specialties  TEXT[]      NOT NULL DEFAULT '{}',
  certificates TEXT,
  created_at   TIMESTAMPTZ NOT NULL,
  updated_at   TIMESTAMPTZ NOT NULL
);

-- ngày sinh nằm ở user_account (identity), không lặp lại ở đây
CREATE TABLE student_profile
(
  user_id             UUID         NOT NULL PRIMARY KEY,
  self_reported_level VARCHAR(2) CHECK (self_reported_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
  occupation          VARCHAR(100),
  notes               TEXT,
  created_at          TIMESTAMPTZ  NOT NULL,
  updated_at          TIMESTAMPTZ  NOT NULL
);

CREATE TABLE course
(
  id          UUID         NOT NULL PRIMARY KEY,
  name        VARCHAR(150) NOT NULL UNIQUE,
  description TEXT,
  status      VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
  created_at  TIMESTAMPTZ  NOT NULL,
  updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE package
(
  id              UUID         NOT NULL PRIMARY KEY,
  course_id       UUID         NOT NULL REFERENCES course (id),
  name            VARCHAR(150) NOT NULL,
  session_count   INT          NOT NULL CHECK (session_count > 0),
  session_minutes INT          NOT NULL CHECK (session_minutes IN (25, 45, 60, 90)),
  validity_days   INT          NOT NULL CHECK (validity_days > 0),
  price_vnd       BIGINT       NOT NULL CHECK (price_vnd >= 0),
  status          VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
  created_at      TIMESTAMPTZ  NOT NULL,
  updated_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX package_course_idx ON package (course_id);

-- sessions_total, session_minutes, validity_days, price_vnd sao chép từ package lúc ghi danh
CREATE TABLE enrollment
(
  id                           UUID        NOT NULL PRIMARY KEY,
  student_id                   UUID        NOT NULL,
  package_id                   UUID        NOT NULL REFERENCES package (id),
  teacher_id                   UUID        NOT NULL,
  sessions_total               INT         NOT NULL CHECK (sessions_total > 0),
  session_minutes              INT         NOT NULL,
  validity_days                INT         NOT NULL CHECK (validity_days > 0),
  price_vnd                    BIGINT      NOT NULL CHECK (price_vnd >= 0),
  sessions_reserved            INT         NOT NULL DEFAULT 0 CHECK (sessions_reserved >= 0),
  sessions_consumed            INT         NOT NULL DEFAULT 0 CHECK (sessions_consumed >= 0),
  activated_at                 TIMESTAMPTZ,
  expires_at                   TIMESTAMPTZ,
  status                       VARCHAR(20) NOT NULL
    CHECK (status IN ('PENDING_PAYMENT', 'ACTIVE', 'EXHAUSTED', 'EXPIRED', 'CANCELLED')),
  renewal_low_notified_at      TIMESTAMPTZ,
  renewal_expiring_notified_at TIMESTAMPTZ,
  extended_by                  UUID,
  extended_at                  TIMESTAMPTZ,
  version                      BIGINT      NOT NULL DEFAULT 0,
  created_at                   TIMESTAMPTZ NOT NULL,
  updated_at                   TIMESTAMPTZ NOT NULL,
  -- bộ đếm không bao giờ vượt tổng số buổi (SC-007, NFR-REL-02)
  CONSTRAINT enrollment_counter_chk CHECK (sessions_reserved + sessions_consumed <= sessions_total)
);
CREATE INDEX enrollment_student_idx ON enrollment (student_id);
CREATE INDEX enrollment_teacher_idx ON enrollment (teacher_id, status);

CREATE TABLE teacher_assignment_history
(
  id              UUID        NOT NULL PRIMARY KEY,
  enrollment_id   UUID        NOT NULL REFERENCES enrollment (id),
  from_teacher_id UUID,
  to_teacher_id   UUID        NOT NULL,
  changed_by      UUID        NOT NULL,
  reason          TEXT,
  created_at      TIMESTAMPTZ NOT NULL,
  updated_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX teacher_assignment_history_enrollment_idx ON teacher_assignment_history (enrollment_id);
