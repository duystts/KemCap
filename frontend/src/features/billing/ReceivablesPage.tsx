import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { fetchReceivables, type ReceivableItem } from '@/lib/api/billing'
import { Input } from '@/components/ui/input'

const formatVnd = (amount: number) => amount.toLocaleString('vi-VN') + ' đ'

/** Giống cách backend tính tổng công nợ: ghi danh đã hủy và thu vượt không tính. */
const owes = (r: ReceivableItem) => !r.cancelled && r.debtVnd > 0

export default function ReceivablesPage() {
  const [search, setSearch] = useState('')
  const [showAll, setShowAll] = useState(false)

  const {
    data: receivables = [],
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['receivables', showAll],
    queryFn: () => fetchReceivables(showAll ? undefined : { hasDebt: true }),
  })

  const keyword = search.trim().toLowerCase()
  const visible = receivables.filter((r) => (r.studentName ?? '').toLowerCase().includes(keyword))
  const totalDebt = receivables.filter(owes).reduce((sum, r) => sum + r.debtVnd, 0)

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Học phí & công nợ</h1>
          <p className="text-sm text-slate-500 mt-1">
            Theo dõi học viên còn nợ và mở ghi danh để ghi nhận thanh toán.
          </p>
        </div>
        <div className="bg-white px-4 py-3 rounded-xl border border-slate-200 shadow-2xs sm:text-right">
          <div className="text-xs text-slate-500">Tổng công nợ hiện tại</div>
          <div className="text-xl font-bold text-rose-600">
            {isLoading || isError ? '—' : formatVnd(totalDebt)}
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="w-full sm:max-w-md">
          <Input
            placeholder="Tìm theo tên học viên…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <label className="flex items-center gap-2 text-xs font-medium text-slate-700 bg-slate-50 border p-2 rounded-xl cursor-pointer hover:bg-slate-100 transition-colors">
            <input
              type="checkbox"
              checked={showAll}
              onChange={(e) => setShowAll(e.target.checked)}
              className="rounded text-indigo-600 focus:ring-indigo-500"
            />
            Hiện cả ghi danh đã thu đủ hoặc đã hủy
          </label>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-2xs">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-slate-400">Đang tải công nợ…</div>
        ) : isError ? (
          <div className="p-12 text-center text-sm text-rose-600">
            Không tải được danh sách công nợ. Vui lòng thử lại.
          </div>
        ) : visible.length === 0 ? (
          <div className="p-12 text-center text-sm text-slate-500">
            {receivables.length === 0
              ? 'Không có học viên nào còn nợ.'
              : 'Không tìm thấy học viên nào phù hợp điều kiện lọc.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b text-xs uppercase font-semibold text-slate-500">
                <tr>
                  <th className="py-3 px-4">Học viên</th>
                  <th className="py-3 px-4 text-right">Học phí</th>
                  <th className="py-3 px-4 text-right">Đã thu</th>
                  <th className="py-3 px-4 text-right">Còn nợ</th>
                  <th className="py-3 px-4 text-right">Ghi danh</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {visible.map((r) => (
                  <tr key={r.enrollmentId} className="hover:bg-slate-50/70 transition-colors">
                    <td className="py-3.5 px-4">
                      <Link
                        to={`/admin/students/${r.studentId}`}
                        className="font-semibold text-slate-900 hover:text-indigo-700"
                      >
                        {r.studentName || 'Không rõ tên'}
                      </Link>
                      {r.cancelled && (
                        <span className="ml-2 inline-flex px-2 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-600">
                          Đã hủy
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right">{formatVnd(r.amountDueVnd)}</td>
                    <td className="py-3.5 px-4 text-right">{formatVnd(r.amountPaidVnd)}</td>
                    <td
                      className={`py-3.5 px-4 text-right font-bold ${
                        owes(r) ? 'text-rose-600' : 'text-slate-400'
                      }`}
                    >
                      {formatVnd(r.debtVnd)}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <Link
                        to={`/admin/enrollments/${r.enrollmentId}`}
                        className="inline-flex text-xs font-semibold px-3 py-1.5 rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors"
                      >
                        Mở ghi danh
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
