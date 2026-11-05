import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { fetchRevenue } from '@/lib/api/billing'
import { fetchTeacherSessions } from '@/lib/api/scheduling'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { last30Days, lastMonth, thisMonth, type DateRange } from './dateRange'

const formatVnd = (amount: number) => amount.toLocaleString('vi-VN') + ' đ'

const PRESETS: { label: string; range: (today: Date) => DateRange }[] = [
  { label: 'Tháng này', range: thisMonth },
  { label: 'Tháng trước', range: lastMonth },
  { label: '30 ngày qua', range: last30Days },
]

export default function DashboardPage() {
  const [today] = useState(() => new Date())
  const [range, setRange] = useState<DateRange>(() => thisMonth(today))

  // Chuỗi YYYY-MM-DD so sánh được theo thứ tự chữ
  const complete = !!range.from && !!range.to
  const valid = complete && range.from <= range.to

  const revenue = useQuery({
    queryKey: ['reports', 'revenue', range],
    queryFn: () => fetchRevenue(range),
    enabled: valid,
  })
  const teacherSessions = useQuery({
    queryKey: ['reports', 'teacher-sessions', range],
    queryFn: () => fetchTeacherSessions(range),
    enabled: valid,
  })

  const rows = [...(teacherSessions.data ?? [])].sort((a, b) => b.sessionsTaught - a.sessionsTaught)
  const totalSessions = rows.reduce((sum, r) => sum + r.sessionsTaught, 0)

  return (
    <div className="space-y-6 max-w-5xl mx-auto p-4 sm:p-6">
      <div>
        <h1 className="text-2xl tracking-tight text-slate-900">Tổng quan</h1>
        <p className="text-sm text-slate-500 mt-1">
          Doanh thu đã thu, công nợ và số buổi đã dạy theo giáo viên để tính thù lao.
        </p>
      </div>

      <div className="bg-white rounded-xl border p-4 flex flex-wrap items-end gap-3">
        <div className="flex flex-wrap gap-2" role="group" aria-label="Chọn nhanh khoảng thời gian">
          {PRESETS.map((p) => {
            const preset = p.range(today)
            const active = preset.from === range.from && preset.to === range.to
            return (
              <Button
                key={p.label}
                type="button"
                size="sm"
                variant={active ? 'default' : 'outline'}
                aria-pressed={active}
                onClick={() => setRange(preset)}
              >
                {p.label}
              </Button>
            )
          })}
        </div>
        <div className="flex flex-wrap items-end gap-2 sm:ml-auto">
          <label className="text-xs font-medium text-slate-700">
            Từ ngày
            <Input
              type="date"
              value={range.from}
              onChange={(e) => setRange((r) => ({ ...r, from: e.target.value }))}
              className="mt-1 font-normal"
            />
          </label>
          <label className="text-xs font-medium text-slate-700">
            Đến ngày
            <Input
              type="date"
              value={range.to}
              onChange={(e) => setRange((r) => ({ ...r, to: e.target.value }))}
              className="mt-1 font-normal"
            />
          </label>
        </div>
      </div>

      {!valid ? (
        <p role="alert" className="text-sm text-rose-600">
          {complete ? 'Ngày bắt đầu phải trước ngày kết thúc.' : 'Hãy chọn đủ từ ngày và đến ngày.'}
        </p>
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <section className="bg-white rounded-xl border p-5">
              <h2 className="text-sm text-slate-600">Đã thu trong kỳ</h2>
              <p className="text-3xl font-semibold text-slate-900 mt-1">
                {revenue.data ? formatVnd(revenue.data.collectedVnd) : '—'}
              </p>
              <p className="text-xs text-slate-500 mt-1">
                Theo ngày thanh toán, gồm cả điều chỉnh âm.
              </p>
            </section>
            <section className="bg-white rounded-xl border p-5">
              <h2 className="text-sm text-slate-600">Công nợ hiện tại</h2>
              <p className="text-3xl font-semibold text-slate-900 mt-1">
                {revenue.data ? formatVnd(revenue.data.debtVnd) : '—'}
              </p>
              <p className="text-xs text-slate-500 mt-1">
                Số dư đến hiện tại, không đổi theo kỳ.{' '}
                <Link to="/admin/billing" className="text-indigo-700 hover:underline">
                  Xem danh sách
                </Link>
              </p>
            </section>
          </div>
          {revenue.isError && (
            <p role="alert" className="text-sm text-rose-600">
              Không tải được doanh thu. Vui lòng thử lại.
            </p>
          )}

          <section className="bg-white rounded-xl border overflow-hidden">
            <div className="p-5 pb-3">
              <h2 className="text-base text-slate-900">Số buổi đã dạy theo giáo viên</h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Gồm buổi hoàn thành, học viên vắng và hủy muộn, tức mọi buổi học viên bị trừ.
              </p>
            </div>
            {teacherSessions.isLoading ? (
              <p className="px-5 pb-5 text-sm text-slate-400">Đang tải…</p>
            ) : teacherSessions.isError ? (
              <p role="alert" className="px-5 pb-5 text-sm text-rose-600">
                Không tải được số buổi đã dạy. Vui lòng thử lại.
              </p>
            ) : rows.length === 0 ? (
              <p className="px-5 pb-5 text-sm text-slate-500">
                Không có buổi nào đã dạy trong kỳ này.
              </p>
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 border-y text-xs font-semibold text-slate-500">
                  <tr>
                    <th className="py-2.5 px-5">Giáo viên</th>
                    <th className="py-2.5 px-5 text-right">Số buổi</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {rows.map((r) => (
                    <tr key={r.teacherId}>
                      <td className="py-3 px-5 text-slate-900">
                        {r.teacherName || 'Không rõ tên'}
                      </td>
                      <td className="py-3 px-5 text-right tabular-nums">{r.sessionsTaught}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot className="border-t font-semibold">
                  <tr>
                    <td className="py-3 px-5 text-slate-900">Tổng</td>
                    <td className="py-3 px-5 text-right tabular-nums">{totalSessions}</td>
                  </tr>
                </tfoot>
              </table>
            )}
          </section>
        </>
      )}
    </div>
  )
}
