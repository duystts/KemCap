-- U7 (R1): khóa lạc quan, request đọc hồ sơ trước lúc ẩn danh hóa không ghi đè định danh trở lại (FR-031)
ALTER TABLE user_account ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
