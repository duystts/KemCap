import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/app/AuthProvider'
import { fetchEnrollments } from '@/lib/api/catalog'
import { fetchMyFeedback } from '@/lib/api/learning'
import { fetchSessions } from '@/lib/api/scheduling'
import { DateTimeText } from '@/components/DateTimeText'
import SessionFeedbackPrompt from './SessionFeedbackPrompt'
import { canRate } from './canRate'

const UPCOMING_LIMIT = 3

/** Trang chủ học viên: buổi sắp tới, số buổi còn lại và lời mời đánh giá các buổi vừa học. */
export default function StudentHomePage() {
  const { me } = useAuth()
  const [now] = useState(() => Date.now())

  const { data: sessions = [], isLoading: loadingSessions } = useQuery({
    queryKey: ['my-sessions', 'completed'],
    queryFn: () => fetchSessions({ status: 'COMPLETED' }),
  })
  const { data: mine = [], isLoading: loadingMine } = useQuery({
    queryKey: ['session-feedback', 'mine'],
    queryFn: fetchMyFeedback,
  })
  const { data: booked = [], isLoading: loadingBooked } = useQuery({
    queryKey: ['my-sessions', 'booked'],
    queryFn: () => fetchSessions({ status: 'BOOKED' }),
  })
  const { data: enrollments = [], isLoading: loadingEnrollments } = useQuery({
    queryKey: ['my-enrollments', me?.id],
    queryFn: () => fetchEnrollments({ studentId: me?.id }),
    enabled: !!me?.id,
  })

  const rated = new Set(mine.map((f) => f.sessionId))
  const toRate = sessions
    .filter((s) => canRate(s, now) && !rated.has(s.id))
    .sort((a, b) => b.endsAt.localeCompare(a.endsAt))
  const upcoming = booked
    .filter((s) => new Date(s.endsAt).getTime() > now)
    .sort((a, b) => a.startsAt.localeCompare(b.startsAt))
    .slice(0, UPCOMING_LIMIT)
  const active = enrollments.filter((e) => e.status === 'ACTIVE')
  const remaining = active.reduce((sum, e) => sum + e.sessionsRemaining, 0)

  return (
    <div className="space-y-6 max-w-3xl mx-auto p-4 sm:p-6">
      <h1 className="text-2xl tracking-tight text-slate-900">Trang chủ</h1>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <section className="sm:col-span-2 bg-white rounded-xl border p-5 space-y-2">
          <h2 className="text-base text-slate-900">Buổi học sắp tới</h2>
          {loadingBooked ? (
            <p className="text-sm text-slate-400">Đang tải…</p>
          ) : upcoming.length === 0 ? (
            <p className="text-sm text-slate-500">
              Chưa có buổi nào.{' '}
              <Link to="/student/book" className="text-indigo-700 hover:underline">
                Đặt lịch
              </Link>
            </p>
          ) : (
            <ul className="divide-y">
              {upcoming.map((s) => (
                <li key={s.id}>
                  <Link
                    to={`/sessions/${s.id}`}
                    className="flex items-center justify-between gap-3 py-2 text-sm text-slate-800 hover:text-indigo-700"
                  >
                    <DateTimeText value={s.startsAt} />
                    <span className="text-xs font-medium text-indigo-700">Xem chi tiết</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="bg-white rounded-xl border p-5">
          <h2 className="text-base text-slate-900">Buổi còn lại</h2>
          {loadingEnrollments ? (
            <p className="text-sm text-slate-400 mt-2">Đang tải…</p>
          ) : active.length === 0 ? (
            <p className="text-sm text-slate-500 mt-2">Chưa có gói đang dùng.</p>
          ) : (
            <p className="text-3xl font-semibold text-slate-900 mt-1">{remaining}</p>
          )}
          <Link to="/student/packages" className="text-xs text-indigo-700 hover:underline">
            Xem gói học
          </Link>
        </section>
      </div>

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
