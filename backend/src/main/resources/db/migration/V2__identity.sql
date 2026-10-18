-- Module identity: tài khoản, token, đồng ý chính sách, đồng ý người giám hộ, yêu cầu chủ thể dữ liệu

CREATE TABLE user_account
(
  id            UUID         NOT NULL PRIMARY KEY,
  email         VARCHAR(255) NOT NULL,
  password_hash VARCHAR(100),
  full_name     VARCHAR(150) NOT NULL,
  phone         VARCHAR(20),
  role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT')),
  timezone      VARCHAR(50)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  date_of_birth DATE,
  status        VARCHAR(30)  NOT NULL
    CHECK (status IN ('AWAITING_GUARDIAN_CONSENT', 'INVITED', 'ACTIVE', 'INACTIVE')),
  last_login_at TIMESTAMPTZ,
  anonymized_at TIMESTAMPTZ,
  created_at    TIMESTAMPTZ  NOT NULL,
  updated_at    TIMESTAMPTZ  NOT NULL,
  -- ngày sinh bắt buộc khi role = STUDENT (FR-030), null với role khác; bị xóa khi ẩn danh hóa (FR-031)
  CONSTRAINT user_account_dob_chk CHECK (
    (role = 'STUDENT' AND (date_of_birth IS NOT NULL OR anonymized_at IS NOT NULL))
      OR (role <> 'STUDENT' AND date_of_birth IS NULL))
);
CREATE UNIQUE INDEX user_account_email_uq ON user_account (lower(email));
CREATE INDEX user_account_role_status_idx ON user_account (role, status);

CREATE TABLE auth_token
(
  id         UUID         NOT NULL PRIMARY KEY,
  user_id    UUID         NOT NULL REFERENCES user_account (id),
  type       VARCHAR(20)  NOT NULL CHECK (type IN ('INVITE', 'PASSWORD_RESET', 'REFRESH')),
  token_hash VARCHAR(100) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ  NOT NULL,
  used_at    TIMESTAMPTZ,
  created_at TIMESTAMPTZ  NOT NULL,
  updated_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX auth_token_user_type_idx ON auth_token (user_id, type);

CREATE TABLE policy_consent
(
  id             UUID        NOT NULL PRIMARY KEY,
  user_id        UUID        NOT NULL REFERENCES user_account (id),
  policy_version VARCHAR(20) NOT NULL,
  accepted_at    TIMESTAMPTZ NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL,
  updated_at     TIMESTAMPTZ NOT NULL,
  CONSTRAINT policy_consent_user_version_uq UNIQUE (user_id, policy_version)
);

CREATE TABLE guardian_consent
(
  id               UUID         NOT NULL PRIMARY KEY,
  student_id       UUID         NOT NULL UNIQUE REFERENCES user_account (id),
  guardian_name    VARCHAR(150) NOT NULL,
  relationship     VARCHAR(50)  NOT NULL,
  guardian_contact VARCHAR(255) NOT NULL,
  consented_on     DATE         NOT NULL,
  recorded_by      UUID         NOT NULL REFERENCES user_account (id),
  created_at       TIMESTAMPTZ  NOT NULL,
  updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE TABLE data_subject_request
(
  id              UUID         NOT NULL PRIMARY KEY,
  student_id      UUID         NOT NULL REFERENCES user_account (id),
  requester_name  VARCHAR(150) NOT NULL,
  type            VARCHAR(20)  NOT NULL CHECK (type IN ('ERASURE', 'ACCESS')),
  received_on     DATE         NOT NULL,
  status          VARCHAR(20)  NOT NULL CHECK (status IN ('RECEIVED', 'COMPLETED', 'REJECTED')),
  resolved_at     TIMESTAMPTZ,
  resolution_note TEXT,
  handled_by      UUID REFERENCES user_account (id),
  created_at      TIMESTAMPTZ  NOT NULL,
  updated_at      TIMESTAMPTZ  NOT NULL,
  CONSTRAINT dsr_reject_note_chk CHECK (status <> 'REJECTED' OR resolution_note IS NOT NULL)
);
CREATE INDEX data_subject_request_student_idx ON data_subject_request (student_id);
