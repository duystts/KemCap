import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { fetchSessionReports } from '@/lib/api/learning'
import { DateTimeText } from '@/components/DateTimeText'

/** Trang chủ giáo viên: các phiếu đánh giá chưa nộp, phiếu sắp hết hạn trước. */
export default function PendingReportsList() {
  const {
    data: reports = [],
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['session-reports', 'PENDING'],
    queryFn: () => fetchSessionReports('PENDING'),
  })

  const sorted = [...reports].sort((a, b) => a.dueAt.localeCompare(b.dueAt))

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Phiếu đánh giá cần nộp</h1>
        <p className="text-sm text-slate-500 mt-1">
          Mỗi buổi đã hoàn thành có một phiếu, hạn nộp 24 giờ sau khi buổi kết thúc.
        </p>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-2xs">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-slate-400">Đang tải phiếu…</div>
        ) : isError ? (
          <div className="p-12 text-center text-sm text-rose-600">
            Không tải được danh sách phiếu. Vui lòng thử lại.
          </div>
        ) : sorted.length === 0 ? (
          <div className="p-12 text-center text-sm text-slate-500">
            Bạn đã nộp đủ phiếu đánh giá.
          </div>
        ) : (
          <ul className="divide-y divide-slate-100">
            {sorted.map((r) => (
              <li key={r.sessionId}>
                <Link
                  to={`/sessions/${r.sessionId}`}
                  className="flex items-center justify-between gap-4 p-4 hover:bg-slate-50/70 transition-colors"
                >
                  <div>
                    <div className="font-semibold text-slate-900 text-sm">
                      {r.studentName || 'Không rõ tên'}
                    </div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      Buổi kết thúc: <DateTimeText value={r.sessionEndsAt} /> · Hạn nộp:{' '}
                      <DateTimeText value={r.dueAt} />
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {r.overdue && (
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-700">
                        Quá hạn
                      </span>
                    )}
                    <span className="text-xs font-semibold text-indigo-700">Điền phiếu</span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
