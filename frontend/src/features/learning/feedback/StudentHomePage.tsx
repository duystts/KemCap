import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchMyFeedback } from '@/lib/api/learning'
import { fetchSessions } from '@/lib/api/scheduling'
import SessionFeedbackPrompt from './SessionFeedbackPrompt'
import { canRate } from './canRate'

/** Trang chủ học viên: mời đánh giá các buổi vừa học (T122). */
export default function StudentHomePage() {
  const [now] = useState(() => Date.now())

  const { data: sessions = [], isLoading: loadingSessions } = useQuery({
    queryKey: ['my-sessions', 'completed'],
    queryFn: () => fetchSessions({ status: 'COMPLETED' }),
  })
  const { data: mine = [], isLoading: loadingMine } = useQuery({
    queryKey: ['session-feedback', 'mine'],
    queryFn: fetchMyFeedback,
  })

  const rated = new Set(mine.map((f) => f.sessionId))
  const toRate = sessions
    .filter((s) => canRate(s, now) && !rated.has(s.id))
    .sort((a, b) => b.endsAt.localeCompare(a.endsAt))

  return (
    <div className="space-y-6 max-w-3xl mx-auto p-4 sm:p-6">
      <h1 className="text-2xl tracking-tight text-slate-900">Trang chủ</h1>
      {loadingSessions || loadingMine ? (
        <p className="text-sm text-slate-400">Đang tải…</p>
      ) : toRate.length === 0 ? (
        <p className="text-sm text-slate-500">Không có buổi học nào cần đánh giá.</p>
      ) : (
        <div className="space-y-3">
          {toRate.map((s) => (
            <SessionFeedbackPrompt key={s.id} session={s} />
          ))}
        </div>
      )}
    </div>
  )
}
