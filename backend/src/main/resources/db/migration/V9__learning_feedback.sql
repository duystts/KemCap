-- Module learning: học viên đánh giá buổi học (FR-022)

CREATE TABLE session_feedback
(
  id         UUID        NOT NULL PRIMARY KEY,
  session_id UUID        NOT NULL UNIQUE,
  student_id UUID        NOT NULL,
  teacher_id UUID        NOT NULL,
  stars      SMALLINT    NOT NULL CHECK (stars BETWEEN 1 AND 5),
  comment    TEXT,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX session_feedback_student_idx ON session_feedback (student_id);
