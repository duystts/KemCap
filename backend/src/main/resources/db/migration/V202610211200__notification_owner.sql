-- U5: liên kết bản ghi thông báo với người dùng để ẩn danh hóa được (data-model.md, FR-031)
-- user_id: người nhận email (null với email mời/đặt lại mật khẩu cũ)
ALTER TABLE notification_dispatch ADD COLUMN user_id UUID;
CREATE INDEX notification_dispatch_user_idx ON notification_dispatch (user_id);

-- about_student_id: học viên được nhắc tới trong thông báo gửi cho giáo viên/admin
ALTER TABLE notification ADD COLUMN about_student_id UUID;
CREATE INDEX notification_about_student_idx ON notification (about_student_id);
