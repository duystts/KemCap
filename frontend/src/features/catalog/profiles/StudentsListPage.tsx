import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/app/AuthProvider'
import { fetchStudents, type StudentSummary } from '@/lib/api/catalog'
import { StatusBadge } from '@/components/StatusBadge'
import { Input } from '@/components/ui/input'
import { Users, Search, AlertCircle, ExternalLink, Filter } from 'lucide-react'

export default function StudentsListPage() {
  const { me } = useAuth()
  const isAdmin = me?.role === 'ADMIN'
  const isTeacher = me?.role === 'TEACHER'

  const [search, setSearch] = useState('')
  const [needsConsultingOnly, setNeedsConsultingOnly] = useState(false)

  const { data: students = [], isLoading } = useQuery({
    queryKey: ['students', search, needsConsultingOnly, isTeacher ? me?.id : undefined],
    queryFn: () =>
      fetchStudents({
        q: search || undefined,
        teacherId: isTeacher ? me?.id : undefined,
        needsConsulting: needsConsultingOnly ? true : undefined,
      }),
  })

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Users className="w-6 h-6 text-indigo-600" />
            {isAdmin ? 'Danh sách học viên toàn hệ thống' : 'Học viên của tôi'}
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Theo dõi danh sách học viên, hồ sơ năng lực CEFR, tiến độ lộ trình và cảnh báo cần tư
            vấn.
          </p>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative w-full sm:max-w-md">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <Input
            placeholder="Tìm theo tên hoặc email học viên…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <label className="flex items-center gap-2 text-xs font-medium text-slate-700 bg-slate-50 border p-2 rounded-xl cursor-pointer hover:bg-slate-100 transition-colors">
            <Filter className="w-3.5 h-3.5 text-slate-500" />
            <input
              type="checkbox"
              checked={needsConsultingOnly}
              onChange={(e) => setNeedsConsultingOnly(e.target.checked)}
              className="rounded text-indigo-600 focus:ring-indigo-500"
            />
            Chỉ hiện học viên &quot;Cần tư vấn&quot;
          </label>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-2xs">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-slate-400">Đang tải học viên…</div>
        ) : students.length === 0 ? (
          <div className="p-12 text-center text-sm text-slate-500">
            Không tìm thấy học viên nào phù hợp điều kiện lọc.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b text-xs uppercase font-semibold text-slate-500">
                <tr>
                  <th className="py-3 px-4">Học viên</th>
                  <th className="py-3 px-4">Trình độ tự khai</th>
                  <th className="py-3 px-4">Trạng thái hồ sơ</th>
                  <th className="py-3 px-4">Tình trạng tư vấn</th>
                  <th className="py-3 px-4 text-right">Xem hồ sơ</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {students.map((s: StudentSummary) => {
                  const profileUrl = isAdmin
                    ? `/admin/students/${s.id}`
                    : `/teacher/students/${s.id}`
                  return (
                    <tr key={s.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-slate-900">{s.fullName}</div>
                        <div className="text-xs text-slate-500">{s.email}</div>
                      </td>
                      <td className="py-3.5 px-4">
                        {s.selfReportedLevel ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded font-bold text-xs bg-indigo-50 text-indigo-700 border border-indigo-200">
                            CEFR {s.selfReportedLevel}
                          </span>
                        ) : (
                          <span className="text-xs text-slate-400 italic">Chưa khai báo</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4">
                        <StatusBadge status={s.status} />
                      </td>
                      <td className="py-3.5 px-4">
                        {s.needsConsulting ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200">
                            <AlertCircle className="w-3.5 h-3.5 text-amber-600" />
                            Cần tư vấn mục tiêu
                          </span>
                        ) : (
                          <span className="text-xs text-emerald-600 font-medium">
                            Đã có mục tiêu
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <Link
                          to={profileUrl}
                          className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors"
                        >
                          Hồ sơ chi tiết
                          <ExternalLink className="w-3.5 h-3.5" />
                        </Link>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
