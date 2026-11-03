-- Module learning: phiếu đánh giá buổi học, bài tập, bài nộp (FR-019, FR-020; K2: chỉ văn bản)

CREATE TABLE session_report
(
  id                  UUID        NOT NULL PRIMARY KEY,
  session_id          UUID        NOT NULL UNIQUE,
  teacher_id          UUID        NOT NULL,
  student_id          UUID        NOT NULL,
  status              VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUBMITTED')),
  session_ends_at     TIMESTAMPTZ NOT NULL,
  due_at              TIMESTAMPTZ NOT NULL,
  taught_topic_ids    UUID[],
  score_listening     SMALLINT CHECK (score_listening BETWEEN 1 AND 5),
  score_speaking      SMALLINT CHECK (score_speaking BETWEEN 1 AND 5),
  score_reading       SMALLINT CHECK (score_reading BETWEEN 1 AND 5),
  score_writing       SMALLINT CHECK (score_writing BETWEEN 1 AND 5),
  score_grammar_vocab SMALLINT CHECK (score_grammar_vocab BETWEEN 1 AND 5),
  comment             TEXT,
  submitted_at        TIMESTAMPTZ,
  overdue_notified_at TIMESTAMPTZ,
  -- buổi đã nộp phiếu được sửa thành không hoàn thành: giữ phiếu, không tính vào tiến bộ
  session_voided_at   TIMESTAMPTZ,
  version             BIGINT      NOT NULL DEFAULT 0,
  created_at          TIMESTAMPTZ NOT NULL,
  updated_at          TIMESTAMPTZ NOT NULL
);
CREATE INDEX session_report_teacher_idx ON session_report (teacher_id, status);
CREATE INDEX session_report_student_idx ON session_report (student_id, session_ends_at);
CREATE INDEX session_report_overdue_idx ON session_report (status, due_at);

CREATE TABLE assignment
(
  id                UUID         NOT NULL PRIMARY KEY,
  session_report_id UUID         NOT NULL REFERENCES session_report (id),
  session_id        UUID         NOT NULL,
  student_id        UUID         NOT NULL,
  teacher_id        UUID         NOT NULL,
  title             VARCHAR(200) NOT NULL,
  instructions      TEXT,
  due_at            TIMESTAMPTZ,
  created_at        TIMESTAMPTZ  NOT NULL,
  updated_at        TIMESTAMPTZ  NOT NULL
);
CREATE INDEX assignment_student_idx ON assignment (student_id, created_at);

CREATE TABLE submission
(
  id            UUID        NOT NULL PRIMARY KEY,
  assignment_id UUID        NOT NULL UNIQUE REFERENCES assignment (id) ON DELETE CASCADE,
  text_answer   TEXT        NOT NULL,
  submitted_at  TIMESTAMPTZ NOT NULL,
  score         SMALLINT CHECK (score BETWEEN 0 AND 10),
  feedback      TEXT,
  graded_by     UUID,
  graded_at     TIMESTAMPTZ,
  version       BIGINT      NOT NULL DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL
);
