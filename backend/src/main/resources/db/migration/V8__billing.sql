-- Module billing: công nợ theo ghi danh + thanh toán chỉ thêm (không sửa/xóa — SC-007, NFR-REL-03)

CREATE TABLE receivable
(
  enrollment_id   UUID        NOT NULL PRIMARY KEY,
  student_id      UUID        NOT NULL,
  amount_due_vnd  BIGINT      NOT NULL CHECK (amount_due_vnd >= 0),
  amount_paid_vnd BIGINT      NOT NULL DEFAULT 0,
  created_at      TIMESTAMPTZ NOT NULL,
  updated_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX receivable_student_idx ON receivable (student_id);

CREATE TABLE payment
(
  id            UUID        NOT NULL PRIMARY KEY,
  enrollment_id UUID        NOT NULL REFERENCES receivable (enrollment_id),
  student_id    UUID        NOT NULL,
  amount_vnd    BIGINT      NOT NULL,
  paid_at       DATE        NOT NULL,
  method        VARCHAR(20) NOT NULL CHECK (method IN ('BANK_TRANSFER', 'CASH', 'OTHER')),
  is_adjustment BOOLEAN     NOT NULL DEFAULT FALSE,
  note          TEXT,
  recorded_by   UUID        NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL,
  updated_at    TIMESTAMPTZ NOT NULL,
  -- bản ghi thường > 0; điều chỉnh (nhập nhầm) phải có ghi chú
  CONSTRAINT payment_amount_chk CHECK (is_adjustment OR amount_vnd > 0),
  CONSTRAINT payment_adjustment_note_chk CHECK (NOT is_adjustment OR note IS NOT NULL)
);
CREATE INDEX payment_enrollment_idx ON payment (enrollment_id, created_at);
