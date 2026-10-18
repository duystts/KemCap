-- Module scheduling: nhật ký chốt/sửa kết quả buổi học (ai đổi gì, lúc nào, vì sao) — NFR-PRIV-04
-- (verify U3 R2). Chỉ thêm, không sửa/xóa.

CREATE TABLE session_outcome_change
(
  id          UUID        NOT NULL PRIMARY KEY,
  session_id  UUID        NOT NULL REFERENCES session (id),
  from_status VARCHAR(20) NOT NULL,
  to_status   VARCHAR(20) NOT NULL,
  changed_by  UUID        NOT NULL,
  note        TEXT,
  created_at  TIMESTAMPTZ NOT NULL,
  updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX session_outcome_change_session_idx ON session_outcome_change (session_id, created_at);
