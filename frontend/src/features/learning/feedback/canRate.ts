import type { SessionItem } from '@/lib/api/scheduling'

const WINDOW_MS = 7 * 24 * 3600 * 1000

/** Buổi còn được đánh giá: hoàn thành và chưa quá 7 ngày sau khi kết thúc (FR-022). */
export function canRate(session: SessionItem, now: number) {
  return session.status === 'COMPLETED' && new Date(session.endsAt).getTime() + WINDOW_MS >= now
}
