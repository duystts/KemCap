import { useAuth } from '@/app/AuthProvider'

export interface DateTimeTextProps {
  /** ISO-8601 từ API (server lưu UTC) */
  value: string | null | undefined
  /** 'datetime' | 'date' | 'time' */
  mode?: 'datetime' | 'date' | 'time'
  /** ghi đè múi giờ người xem */
  timeZone?: string
}

const OPTIONS: Record<NonNullable<DateTimeTextProps['mode']>, Intl.DateTimeFormatOptions> = {
  datetime: { dateStyle: 'short', timeStyle: 'short' },
  date: { dateStyle: 'medium' },
  time: { timeStyle: 'short' },
}

export function formatDateTime(
  value: string,
  timeZone: string,
  mode: NonNullable<DateTimeTextProps['mode']> = 'datetime',
) {
  return new Intl.DateTimeFormat('vi-VN', { ...OPTIONS[mode], timeZone }).format(new Date(value))
}

/** Hiển thị thời gian theo múi giờ người xem (constitution: lưu UTC, hiển thị theo người xem). */
export function DateTimeText({ value, mode = 'datetime', timeZone }: DateTimeTextProps) {
  const { me } = useAuth()
  if (!value) return <span className="text-muted-foreground">—</span>
  const tz = timeZone ?? me?.timezone ?? 'Asia/Ho_Chi_Minh'
  return <time dateTime={value}>{formatDateTime(value, tz, mode)}</time>
}
