-- Module learning: giáo vụ đóng phiếu đánh giá treo khi giáo viên của buổi không còn phụ trách học
-- viên, ví dụ đã nghỉ việc (T148, U6 R5). Phiếu đóng không còn bị nhắc quá hạn.

ALTER TABLE session_report DROP CONSTRAINT session_report_status_check;
ALTER TABLE session_report
  ADD CONSTRAINT session_report_status_check CHECK (status IN ('PENDING', 'SUBMITTED', 'CLOSED')),
  ADD COLUMN close_reason TEXT,
  ADD COLUMN closed_by    UUID,
  ADD COLUMN closed_at    TIMESTAMPTZ;
