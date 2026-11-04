import { useCallback } from 'react'
import { toast } from 'sonner'
import { ApiError } from '@/lib/api/client'

/** Thông điệp tiếng Việt theo `code` nghiệp vụ (bảng cuối contracts/rest-api.md). */
export const ERROR_MESSAGES: Record<string, string> = {
  VALIDATION_FAILED: 'Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại.',
  UNAUTHENTICATED: 'Bạn chưa đăng nhập hoặc thông tin đăng nhập không đúng.',
  TOKEN_EXPIRED: 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
  CONSENT_REQUIRED: 'Bạn cần đồng ý điều khoản và chính sách dữ liệu hiện hành.',
  ROOM_NOT_OPEN: 'Phòng học chỉ mở từ 10 phút trước giờ học đến 15 phút sau giờ kết thúc.',
  NOT_FOUND: 'Không tìm thấy dữ liệu.',
  SLOT_TAKEN: 'Khung giờ này vừa có người đặt, vui lòng chọn giờ khác.',
  NO_SESSIONS_LEFT: 'Gói học đã hết buổi. Vui lòng liên hệ giáo vụ để gia hạn.',
  ENROLLMENT_NOT_ACTIVE: 'Gói học chưa kích hoạt (chờ thanh toán), đã hết hạn hoặc đã hủy.',
  CANCEL_WINDOW_PASSED: 'Đã quá hạn chót nên không đổi lịch được. Bạn vẫn có thể hủy muộn.',
  INVALID_STATE_TRANSITION: 'Thao tác không hợp lệ với trạng thái hiện tại.',
  EMAIL_TAKEN: 'Email này đã được sử dụng.',
  GUARDIAN_CONSENT_REQUIRED: 'Học viên dưới 16 tuổi cần đồng ý của cha mẹ/người giám hộ.',
  TEACHER_HAS_STUDENTS: 'Giáo viên còn học viên được phân công — hãy đổi giáo viên trước.',
  ANONYMIZED: 'Hồ sơ đã được ẩn danh hóa.',
  EXCEPTION_OVERLAPS_SESSIONS: 'Ngày nghỉ trùng buổi học đã đặt — hãy hủy các buổi đó trước.',
  FEEDBACK_EXISTS: 'Bạn đã đánh giá buổi học này.',
  FEEDBACK_WINDOW_CLOSED: 'Đã quá 7 ngày nên không đánh giá được nữa.',
  CONCURRENT_MODIFICATION: 'Dữ liệu vừa được người khác thay đổi, vui lòng tải lại.',
  INTERNAL_ERROR: 'Lỗi hệ thống, vui lòng thử lại sau.',
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    // 1. Nếu có lỗi theo từng trường cụ thể (Validation fields)
    if (error.errors && error.errors.length > 0) {
      const first = error.errors[0]
      return `${first.message || 'Dữ liệu không hợp lệ'} (${first.field})`
    }
    // 2. Nếu server có thông điệp giải thích cụ thể
    if (
      error.message &&
      error.message !== 'Bad Request' &&
      error.message !== 'Conflict' &&
      error.message !== error.code
    ) {
      return error.message
    }
    return ERROR_MESSAGES[error.code] ?? error.message ?? 'Đã có lỗi xảy ra.'
  }
  if (error instanceof TypeError) return 'Không kết nối được máy chủ.'
  return 'Đã có lỗi xảy ra.'
}

export function useApiErrorToast() {
  return useCallback((error: unknown) => {
    toast.error(errorMessage(error))
  }, [])
}
