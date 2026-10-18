-- Module learning (hồ sơ): đánh giá năng lực, mục tiêu học tập, lộ trình → chặng → chủ đề, lịch sử điều chỉnh

-- chỉ thêm: hồ sơ hiện tại = bản mới nhất, lịch sử = toàn bộ (FR-016, NFR-PORT-03)
CREATE TABLE proficiency_assessment
(
  id            UUID        NOT NULL PRIMARY KEY,
  student_id    UUID        NOT NULL,
  type          VARCHAR(20) NOT NULL CHECK (type IN ('PLACEMENT', 'PERIODIC')),
  assessed_by   UUID        NOT NULL,
  assessed_at   DATE        NOT NULL,
  listening     VARCHAR(2)  NOT NULL CHECK (listening IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
  speaking      VARCHAR(2)  NOT NULL CHECK (speaking IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
  reading       VARCHAR(2)  NOT NULL CHECK (reading IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
  writing       VARCHAR(2)  NOT NULL CHECK (writing IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
  grammar_vocab VARCHAR(2)  NOT NULL CHECK (grammar_vocab IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
  notes         TEXT,
  created_at    TIMESTAMPTZ NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX proficiency_assessment_student_idx ON proficiency_assessment (student_id, assessed_at);

-- 0..n mỗi học viên; không có goal ACTIVE + đang có ghi danh = "Cần tư vấn"
CREATE TABLE learning_goal
(
  id         UUID         NOT NULL PRIMARY KEY,
  student_id UUID         NOT NULL,
  type       VARCHAR(20)  NOT NULL
    CHECK (type IN ('IELTS', 'TOEIC', 'COMMUNICATION', 'BUSINESS', 'KIDS', 'OTHER')),
  target     VARCHAR(100) NOT NULL,
  deadline   DATE,
  status     VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'ACHIEVED', 'DROPPED')),
  created_by UUID         NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL,
  updated_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX learning_goal_student_idx ON learning_goal (student_id, status);

CREATE TABLE learning_path
(
  id         UUID         NOT NULL PRIMARY KEY,
  student_id UUID         NOT NULL,
  created_by UUID         NOT NULL,
  title      VARCHAR(200) NOT NULL,
  status     VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'ARCHIVED')),
  version    BIGINT       NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ  NOT NULL,
  updated_at TIMESTAMPTZ  NOT NULL
);
-- tối đa một lộ trình ACTIVE mỗi học viên
CREATE UNIQUE INDEX learning_path_one_active_uq ON learning_path (student_id) WHERE status = 'ACTIVE';

CREATE TABLE milestone
(
  id          UUID         NOT NULL PRIMARY KEY,
  path_id     UUID         NOT NULL REFERENCES learning_path (id) ON DELETE CASCADE,
  position    INT          NOT NULL,
  title       VARCHAR(200) NOT NULL,
  target_date DATE,
  status      VARCHAR(20)  NOT NULL CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'DONE')),
  created_at  TIMESTAMPTZ  NOT NULL,
  updated_at  TIMESTAMPTZ  NOT NULL,
  CONSTRAINT milestone_position_uq UNIQUE (path_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE topic
(
  id           UUID         NOT NULL PRIMARY KEY,
  milestone_id UUID         NOT NULL REFERENCES milestone (id) ON DELETE CASCADE,
  position     INT          NOT NULL,
  title        VARCHAR(200) NOT NULL,
  objective    TEXT,
  status       VARCHAR(20)  NOT NULL CHECK (status IN ('PLANNED', 'DONE')),
  created_at   TIMESTAMPTZ  NOT NULL,
  updated_at   TIMESTAMPTZ  NOT NULL,
  CONSTRAINT topic_position_uq UNIQUE (milestone_id, position) DEFERRABLE INITIALLY DEFERRED
);

-- mỗi lần sửa lộ trình sau khi tạo (FR-018): ai sửa, lý do bắt buộc, tóm tắt tự sinh
CREATE TABLE path_revision
(
  id         UUID        NOT NULL PRIMARY KEY,
  path_id    UUID        NOT NULL REFERENCES learning_path (id) ON DELETE CASCADE,
  changed_by UUID        NOT NULL,
  reason     TEXT        NOT NULL,
  summary    TEXT        NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX path_revision_path_idx ON path_revision (path_id, created_at);
