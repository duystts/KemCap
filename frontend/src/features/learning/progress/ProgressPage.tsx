import { lazy, Suspense } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/app/AuthProvider'
import { fetchProficiency, fetchProgress } from '@/lib/api/learning'
import { DateTimeText } from '@/components/DateTimeText'
import { SKILL_SERIES, shortDate } from './skills'

// Recharts nặng nên chỉ tải khi có biểu đồ (NFR-PERF-07)
const SkillChart = lazy(() => import('./SkillChart'))

/** Dùng cho `/student/progress` (của chính học viên) và tab "Tiến bộ" ở hồ sơ học viên. */
export default function ProgressPage({ studentId }: { studentId?: string }) {
  const { me } = useAuth()
  const id = studentId ?? me?.id

  const {
    data: progress,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['student-progress', id],
    queryFn: () => fetchProgress(id!),
    enabled: !!id,
  })
  const { data: proficiency } = useQuery({
    queryKey: ['student-proficiency-summary', id],
    queryFn: () => fetchProficiency(id!),
    enabled: !!id,
  })

  const current = proficiency?.current
  const points = progress?.skillSeries ?? []
  const reports = progress?.recentReports ?? []
  const completion = progress?.pathCompletion

  return (
    <div className={studentId ? 'space-y-6' : 'space-y-6 max-w-5xl mx-auto p-4 sm:p-6'}>
      {!studentId && (
        <div>
          <h1 className="text-2xl tracking-tight text-slate-900">Tiến bộ</h1>
          <p className="text-sm text-slate-500 mt-1">
            Trình độ hiện tại, tiến độ lộ trình và điểm kỹ năng qua từng buổi học.
          </p>
        </div>
      )}

      {isLoading ? (
        <div className="p-12 text-center text-sm text-slate-400">Đang tải tiến bộ…</div>
      ) : isError ? (
        <div className="p-12 text-center text-sm text-rose-600">
          Không tải được dữ liệu tiến bộ. Vui lòng thử lại.
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <section className="md:col-span-2 bg-white rounded-xl border p-5 space-y-3">
              <h2 className="text-base text-slate-900">Trình độ CEFR hiện tại</h2>
              {current ? (
                <>
                  <dl className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                    {SKILL_SERIES.map((f) => (
                      <div key={f.key}>
                        <dt className="text-xs text-slate-500">{f.label}</dt>
                        <dd className="text-xl font-semibold text-slate-900">{current[f.key]}</dd>
                      </div>
                    ))}
                  </dl>
                  <p className="text-xs text-slate-500">
                    Đánh giá ngày <DateTimeText value={current.assessedAt} mode="date" />
                  </p>
                </>
              ) : (
                <p className="text-sm text-slate-500">Chưa có đánh giá năng lực.</p>
              )}
            </section>

            <section className="bg-white rounded-xl border p-5 space-y-2">
              <h2 className="text-base text-slate-900">Hoàn thành lộ trình</h2>
              {completion === null || completion === undefined ? (
                <p className="text-sm text-slate-500">Chưa có lộ trình hoặc chủ đề.</p>
              ) : (
                <>
                  <p className="text-3xl font-semibold text-slate-900">{completion}%</p>
                  <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-indigo-600 rounded-full"
                      style={{ width: `${Math.min(100, Math.max(0, completion))}%` }}
                    />
                  </div>
                </>
              )}
            </section>
          </div>

          <section className="bg-white rounded-xl border p-5 space-y-3">
            <div>
              <h2 className="text-base text-slate-900">Điểm kỹ năng theo buổi học</h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Thang 1–5, lấy từ phiếu đánh giá của giáo viên.
              </p>
            </div>
            {points.length === 0 ? (
              <p className="text-sm text-slate-500">Chưa có phiếu đánh giá nào.</p>
            ) : (
              <>
                <Suspense fallback={<div className="h-40" />}>
                  <SkillChart points={points} />
                </Suspense>
                <details className="text-sm">
                  <summary className="cursor-pointer text-xs font-medium text-slate-600">
                    Xem dạng bảng
                  </summary>
                  <div className="overflow-x-auto mt-2">
                    <table className="w-full text-left text-sm">
                      <thead className="text-xs text-slate-500 border-b">
                        <tr>
                          <th className="py-2 pr-4 font-medium">Ngày</th>
                          {SKILL_SERIES.map((s) => (
                            <th key={s.key} className="py-2 pr-4 font-medium">
                              {s.label}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {points.map((p) => (
                          <tr key={p.sessionId} className="border-b last:border-0">
                            <td className="py-2 pr-4">{shortDate(p.date)}</td>
                            {SKILL_SERIES.map((s) => (
                              <td key={s.key} className="py-2 pr-4">
                                {p[s.key] ?? '—'}
                              </td>
                            ))}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </details>
              </>
            )}
          </section>

          <section className="bg-white rounded-xl border p-5 space-y-3">
            <h2 className="text-base text-slate-900">Nhận xét gần nhất</h2>
            {reports.length === 0 ? (
              <p className="text-sm text-slate-500">Chưa có nhận xét nào.</p>
            ) : (
              <ul className="divide-y">
                {reports.map((r) => (
                  <li key={r.sessionId} className="py-3 first:pt-0 last:pb-0 space-y-1">
                    <p className="text-xs text-slate-500">
                      Buổi <DateTimeText value={r.sessionEndsAt} mode="date" /> ·{' '}
                      <Link
                        to={`/sessions/${r.sessionId}`}
                        className="text-indigo-700 underline underline-offset-2"
                      >
                        Xem buổi học
                      </Link>
                    </p>
                    <p className="text-sm text-slate-800 whitespace-pre-wrap">{r.comment}</p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  )
}
