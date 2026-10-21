import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { useAuth } from '@/app/AuthProvider'
import { fetchEnrollments, type EnrollmentDetail } from '@/lib/api/catalog'
import { StatusBadge } from '@/components/StatusBadge'
import { Card, CardContent } from '@/components/ui/card'
import { Package, Calendar, Clock, ArrowRight } from 'lucide-react'

export default function StudentPackagesPage() {
  const { me } = useAuth()
  const studentId = me?.id || ''

  const { data: enrollments = [], isLoading } = useQuery({
    queryKey: ['my-enrollments', studentId],
    queryFn: () => fetchEnrollments({ studentId }),
    enabled: !!studentId,
  })

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
          <Package className="w-6 h-6 text-indigo-600" />
          Gói học của tôi
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Theo dõi số buổi học khả dụng, số buổi đã hoàn thành và thời hạn của từng gói học.
        </p>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-sm text-slate-400">Đang tải gói học của bạn…</div>
      ) : enrollments.length === 0 ? (
        <Card className="border-dashed bg-slate-50">
          <CardContent className="p-12 text-center space-y-3">
            <Package className="w-8 h-8 text-slate-400 mx-auto" />
            <h3 className="font-semibold text-slate-800">Bạn chưa có gói học nào</h3>
            <p className="text-sm text-slate-500 max-w-sm mx-auto">
              Vui lòng liên hệ với bộ phận tư vấn / giáo vụ để đăng ký gói học phù hợp.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {enrollments.map((enr: EnrollmentDetail) => (
            <div
              key={enr.id}
              className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs hover:border-indigo-200 hover:shadow-md transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Gói học 1 kèm 1
                  </span>
                  <StatusBadge status={enr.status} />
                </div>

                <h3 className="text-lg font-bold text-slate-900">
                  {enr.sessionsTotal} buổi ({enr.sessionMinutes} phút/buổi)
                </h3>

                <div className="grid grid-cols-3 gap-2 py-4 my-3 border-y text-center">
                  <div>
                    <div className="text-[11px] text-slate-500">Còn lại</div>
                    <div className="text-xl font-black text-indigo-600 mt-0.5">
                      {enr.sessionsRemaining}
                    </div>
                  </div>
                  <div>
                    <div className="text-[11px] text-slate-500">Đã học</div>
                    <div className="text-xl font-black text-emerald-600 mt-0.5">
                      {enr.sessionsConsumed}
                    </div>
                  </div>
                  <div>
                    <div className="text-[11px] text-slate-500">Đã đặt</div>
                    <div className="text-xl font-black text-blue-600 mt-0.5">
                      {enr.sessionsReserved}
                    </div>
                  </div>
                </div>

                <div className="space-y-1.5 text-xs text-slate-500">
                  {enr.expiresAt && (
                    <div className="flex items-center gap-1.5">
                      <Calendar className="w-3.5 h-3.5 text-slate-400" />
                      Hết hạn: {new Date(enr.expiresAt).toLocaleDateString('vi-VN')}
                    </div>
                  )}
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    Thời lượng mỗi buổi: {enr.sessionMinutes} phút
                  </div>
                </div>
              </div>

              {enr.status === 'ACTIVE' && enr.sessionsRemaining > 0 && (
                <div className="pt-4 mt-4 border-t">
                  <Link
                    to="/student/book"
                    className="w-full inline-flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-indigo-600 text-white font-semibold text-xs hover:bg-indigo-700 transition-colors shadow-xs"
                  >
                    Đặt lịch ngay
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
