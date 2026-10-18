-- Module notification: thông báo trong hệ thống + nhật ký gửi email (chống trùng, thử lại)

CREATE TABLE notification
(
  id         UUID         NOT NULL PRIMARY KEY,
  user_id    UUID         NOT NULL,
  type       VARCHAR(50)  NOT NULL,
  title      TEXT         NOT NULL,
  body       TEXT,
  link       VARCHAR(300),
  read_at    TIMESTAMPTZ,
  created_at TIMESTAMPTZ  NOT NULL,
  updated_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX notification_user_idx ON notification (user_id, created_at DESC);

CREATE TABLE notification_dispatch
(
  id              UUID         NOT NULL PRIMARY KEY,
  dedupe_key      VARCHAR(200) NOT NULL UNIQUE,
  channel         VARCHAR(20)  NOT NULL CHECK (channel IN ('EMAIL')),
  recipient       VARCHAR(255) NOT NULL,
  -- nội dung đã render, cần để thử lại khi gửi lỗi
  subject         TEXT         NOT NULL,
  body_html       TEXT         NOT NULL,
  status          VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
  attempts        SMALLINT     NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ,
  error           TEXT,
  sent_at         TIMESTAMPTZ,
  created_at      TIMESTAMPTZ  NOT NULL,
  updated_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX notification_dispatch_pending_idx ON notification_dispatch (status, next_attempt_at);
