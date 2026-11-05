import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/app/AuthProvider'
import { fetchSessionReport } from '@/lib/api/learning'
import { ApiError } from '@/lib/api/client'
import { DateTimeText } from '@/components/DateTimeText'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import SessionReportForm from './SessionReportForm'
import { SKILLS } from './skills'

const MAX_WAIT_ATTEMPTS = 5

/** Phiếu của một buổi đã hoàn thành: giáo viên của buổi điền phiếu, sau khi nộp mọi bên chỉ đọc. */
export default function SessionReportSection({ sessionId }: { sessionId: string }) {
  const { me } = useAuth()

  const { data: report, isLoading } = useQuery({
    queryKey: ['session-report', sessionId],
    queryFn: () => fetchSessionReport(sessionId),
    retry: false,
    // Backend sinh phiếu bất đồng bộ sau khi chốt buổi nên có thể chậm một nhịp
    refetchInterval: (query) => {
      const { error, errorUpdateCount } = query.state
      const waiting = error instanceof ApiError && error.status === 404
      return waiting && errorUpdateCount < MAX_WAIT_ATTEMPTS ? 1500 : false
    },
  })

  if (isLoading) {
    return <p className="text-sm text-slate-400">Đang tải phiếu đánh giá…</p>
  }
  if (!report) return null

  if (report.status === 'PENDING') {
    if (report.teacherId === me?.id) return <SessionReportForm report={report} />
    return (
      <p className="text-sm text-slate-500">
        Phiếu đánh giá chưa được nộp. Hạn nộp: <DateTimeText value={report.dueAt} />
      </p>
    )
  }

  return (
    <Card>
      <CardHeader className="pb-3 border-b">
        <CardTitle className="text-base font-bold text-slate-800">
          Phiếu đánh giá buổi học
        </CardTitle>
        <p className="text-xs text-slate-500 mt-0.5">
          Đã nộp lúc <DateTimeText value={report.submittedAt} />
          {report.onTime === false && ' (trễ hạn)'}
        </p>
      </CardHeader>
      <CardContent className="pt-5 space-y-4 text-sm">
        <dl className="grid grid-cols-2 sm:grid-cols-5 gap-3">
          {SKILLS.map((s) => (
            <div key={s.key} className="p-3 rounded-xl bg-slate-50 border">
              <dt className="text-xs text-slate-500">{s.label}</dt>
              <dd className="text-lg font-bold text-indigo-600">{report.scores?.[s.key] ?? '—'}</dd>
            </div>
          ))}
        </dl>
        <p className="text-slate-700 whitespace-pre-wrap">{report.comment}</p>
      </CardContent>
    </Card>
  )
}
