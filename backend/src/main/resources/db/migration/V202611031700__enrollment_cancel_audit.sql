-- U8 (R7 từ verify U2): lưu ai hủy ghi danh, lúc nào, vì sao; cancel_reason bị xóa khi ẩn danh hóa
ALTER TABLE enrollment ADD COLUMN cancel_reason TEXT;
ALTER TABLE enrollment ADD COLUMN cancelled_by UUID;
ALTER TABLE enrollment ADD COLUMN cancelled_at TIMESTAMPTZ;
