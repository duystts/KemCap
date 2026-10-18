import { cn } from '@/lib/utils'

type Tone = 'neutral' | 'success' | 'warning' | 'danger' | 'info'

const TONE_CLASS: Record<Tone, string> = {
  neutral: 'bg-slate-100 text-slate-700',
  success: 'bg-emerald-100 text-emerald-800',
  warning: 'bg-amber-100 text-amber-800',
  danger: 'bg-red-100 text-red-800',
  info: 'bg-blue-100 text-blue-800',
}

/** Nhãn trạng thái tiếng Việt cho mọi enum trạng thái của API. */
const LABELS: Record<string, [string, Tone]> = {
  // tài khoản
  AWAITING_GUARDIAN_CONSENT: ['Chờ đồng ý người giám hộ', 'warning'],
  INVITED: ['Đã mời', 'info'],
  ACTIVE: ['Đang dùng', 'success'],
  INACTIVE: ['Ngừng kích hoạt', 'neutral'],
  // ghi danh
  PENDING_PAYMENT: ['Chờ thanh toán', 'warning'],
  EXHAUSTED: ['Hết buổi', 'neutral'],
  EXPIRED: ['Hết hạn', 'danger'],
  CANCELLED: ['Đã hủy', 'neutral'],
  // buổi học
  BOOKED: ['Đã đặt', 'info'],
  COMPLETED: ['Hoàn thành', 'success'],
  STUDENT_NO_SHOW: ['Học viên vắng', 'danger'],
  TEACHER_ABSENT: ['Giáo viên vắng/hủy', 'warning'],
  CANCELLED_ON_TIME: ['Hủy đúng hạn', 'neutral'],
  CANCELLED_LATE: ['Hủy muộn', 'danger'],
  CANCELLED_SYSTEM: ['Hủy hệ thống', 'neutral'],
  INTERRUPTED: ['Gián đoạn', 'warning'],
}

export function statusLabel(status: string) {
  return LABELS[status]?.[0] ?? status
}

export function StatusBadge({ status, className }: { status: string; className?: string }) {
  const [label, tone] = LABELS[status] ?? [status, 'neutral' as Tone]
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        TONE_CLASS[tone],
        className,
      )}
    >
      {label}
    </span>
  )
}
