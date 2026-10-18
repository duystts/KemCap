package com.englishplatform.shared.web;

import org.springframework.http.HttpStatus;

/** Mã lỗi nghiệp vụ — bảng cuối contracts/rest-api.md. Thông điệp mặc định tiếng Việt. */
public enum ErrorCode {
  VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc thông tin đăng nhập không đúng"),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
  CONSENT_REQUIRED(
      HttpStatus.FORBIDDEN, "Bạn cần đồng ý điều khoản và chính sách dữ liệu hiện hành"),
  ROOM_NOT_OPEN(HttpStatus.FORBIDDEN, "Phòng học chưa mở hoặc đã đóng"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
  SLOT_TAKEN(HttpStatus.CONFLICT, "Khung giờ này vừa có người đặt"),
  NO_SESSIONS_LEFT(HttpStatus.CONFLICT, "Gói học đã hết buổi"),
  ENROLLMENT_NOT_ACTIVE(HttpStatus.CONFLICT, "Gói học chưa kích hoạt, đã hết hạn hoặc đã hủy"),
  CANCEL_WINDOW_PASSED(HttpStatus.CONFLICT, "Đã quá hạn chót đổi lịch"),
  INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "Thao tác không hợp lệ với trạng thái hiện tại"),
  EMAIL_TAKEN(HttpStatus.CONFLICT, "Email đã được sử dụng"),
  GUARDIAN_CONSENT_REQUIRED(
      HttpStatus.CONFLICT, "Học viên dưới 16 tuổi cần đồng ý của cha mẹ/người giám hộ"),
  TEACHER_HAS_STUDENTS(HttpStatus.CONFLICT, "Giáo viên còn học viên được phân công"),
  ANONYMIZED(HttpStatus.CONFLICT, "Hồ sơ đã được ẩn danh hóa"),
  EXCEPTION_OVERLAPS_SESSIONS(HttpStatus.CONFLICT, "Ngày nghỉ trùng buổi học đã đặt"),
  FEEDBACK_EXISTS(HttpStatus.CONFLICT, "Bạn đã đánh giá buổi học này"),
  FEEDBACK_WINDOW_CLOSED(HttpStatus.CONFLICT, "Đã quá hạn đánh giá buổi học"),
  CONCURRENT_MODIFICATION(
      HttpStatus.CONFLICT, "Dữ liệu vừa được người khác thay đổi, vui lòng tải lại");

  private final HttpStatus status;
  private final String defaultMessage;

  ErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  public HttpStatus status() {
    return status;
  }

  public String defaultMessage() {
    return defaultMessage;
  }
}
