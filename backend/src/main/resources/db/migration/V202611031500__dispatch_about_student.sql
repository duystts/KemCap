-- U7 (R3): email chờ gửi lại cho giáo viên/admin có thể nhắc tên học viên; cần biết học viên nào
-- để xóa nội dung khi ẩn danh hóa (FR-031)
ALTER TABLE notification_dispatch ADD COLUMN about_student_id UUID;
CREATE INDEX notification_dispatch_about_student_idx ON notification_dispatch (about_student_id);
