-- FR-031 (review U5 R2, R4): email mời/đặt lại mật khẩu cũ chưa có user_id → lấy từ dedupe_key
-- (user:{userId}:invite|reset:{tokenId}) để ẩn danh hóa tìm được; version chống job gửi lại ghi đè
-- bản đã ẩn danh hóa.
UPDATE notification_dispatch
SET user_id = CAST(split_part(dedupe_key, ':', 2) AS UUID)
WHERE user_id IS NULL
  AND dedupe_key ~ '^user:[0-9a-f-]{36}:(invite|reset):';

ALTER TABLE notification_dispatch ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
