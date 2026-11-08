import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { useAuth } from '@/app/AuthProvider'
import { fetchEnrollments, type EnrollmentDetail } from '@/lib/api/catalog'
import { fetchStatement, PAYMENT_METHOD_LABEL } from '@/lib/api/billing'
import { StatusBadge } from '@/components/StatusBadge'
import { Card, CardContent } from '@/components/ui/card'

// Cùng ngưỡng với job nhắc gia hạn (app.reminders.renewal-low-sessions, renewal-expiring-days)
const RENEWAL_LOW_SESSIONS = 2
const RENEWAL_EXPIRING_DAYS = 7
const DAY_MS = 24 * 3600 * 1000

const formatVnd = (amount: number) => amount.toLocaleString('vi-VN') + ' đ'

/** Ngày YYYY-MM-DD hiển thị nguyên trạng, không qua múi giờ. */
function formatDay(day: string) {
  const [year, month, date] = day.split('-')
  return `${date}/${month}/${year}`
}

function renewalMessage(enrollment: EnrollmentDetail, now: number) {
  if (enrollment.status !== 'ACTIVE') return null
  const low = enrollment.sessionsRemaining <= RENEWAL_LOW_SESSIONS
  const daysLeft = enrollment.expiresAt
    ? Math.ceil((new Date(enrollment.expiresAt).getTime() - now) / DAY_MS)
    : null
  const expiring = daysLeft !== null && daysLeft <= RENEWAL_EXPIRING_DAYS
  if (!low && !expiring) return null
  const reasons = [
    low ? `còn ${enrollment.sessionsRemaining} buổi` : null,
    expiring ? (daysLeft! > 0 ? `còn ${daysLeft} ngày sử dụng` : 'hết hạn hôm nay') : null,
  ].filter(Boolean)
  return `Gói sắp hết: ${reasons.join(', ')}. Hãy liên hệ giáo vụ để gia hạn.`
}

function PaymentSummary({ enrollmentId }: { enrollmentId: string }) {
  const {
    data: statement,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['statement', enrollmentId],
    queryFn: () => fetchStatement(enrollmentId),
  })

  if (isLoading) return <p className="text-xs text-slate-400">Đang tải học phí…</p>
  if (isError || !statement) {
    return <p className="text-xs text-rose-600">Không tải được thông tin học phí.</p>
  }

  return (
    <div className="space-y-2 text-xs">
      <dl className="grid grid-cols-3 gap-2 text-slate-500">
        <div>
          <dt>Học phí</dt>
          <dd className="font-semibold text-slate-900">{formatVnd(statement.amountDueVnd)}</dd>
        </div>
        <div>
          <dt>Đã đóng</dt>
          <dd className="font-semibold text-slate-900">{formatVnd(statement.amountPaidVnd)}</dd>
        </div>
        <div>
          <dt>Còn nợ</dt>
          <dd
            className={`font-semibold ${statement.debtVnd > 0 ? 'text-rose-600' : 'text-slate-900'}`}
          >
            {formatVnd(statement.debtVnd)}
          </dd>
        </div>
      </dl>
      {statement.payments.length === 0 ? (
        <p className="text-slate-500">Chưa có khoản thanh toán nào.</p>
      ) : (
        <table className="w-full text-left">
          <caption className="sr-only">Lịch sử thanh toán</caption>
          <thead className="text-slate-500 border-b">
            <tr>
              <th className="py-1 font-medium">Ngày</th>
              <th className="py-1 font-medium">Hình thức</th>
              <th className="py-1 font-medium text-right">Số tiền</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {statement.payments.map((p) => (
              <tr key={p.id}>
                <td className="py-1">{formatDay(p.paidAt)}</td>
                <td className="py-1">
                  {p.adjustment ? 'Điều chỉnh' : PAYMENT_METHOD_LABEL[p.method]}
                </td>
                <td className="py-1 text-right tabular-nums">{formatVnd(p.amountVnd)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

export default function StudentPackagesPage() {
  const { me } = useAuth()
  const studentId = me?.id || ''
  const [now] = useState(() => Date.now())

  const { data: enrollments = [], isLoading } = useQuery({
    queryKey: ['my-enrollments', studentId],
    queryFn: () => fetchEnrollments({ studentId }),
    enabled: !!studentId,
  })

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
          Gói học của tôi
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Theo dõi số buổi học khả dụng, học phí và thời hạn của từng gói học.
        </p>
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-sm text-slate-400">Đang tải gói học của bạn…</div>
      ) : enrollments.length === 0 ? (
        <Card className="border-dashed bg-slate-50">
          <CardContent className="p-12 text-center space-y-3">
            <h3 className="font-semibold text-slate-800">Bạn chưa có gói học nào</h3>
            <p className="text-sm text-slate-500 max-w-sm mx-auto">
              Vui lòng liên hệ với bộ phận tư vấn / giáo vụ để đăng ký gói học phù hợp.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {enrollments.map((enr: EnrollmentDetail) => {
            const renewal = renewalMessage(enr, now)
            return (
              <div
                key={enr.id}
                className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs flex flex-col justify-between"
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

                  {renewal && (
                    <p
                      role="status"
                      className="mt-3 p-3 rounded-lg bg-amber-50 border border-amber-200 text-xs text-amber-900"
                    >
                      {renewal}
                    </p>
                  )}
                  {enr.status === 'PENDING_PAYMENT' && (
                    <p className="mt-3 p-3 rounded-lg bg-slate-50 border text-xs text-slate-700">
                      Gói sẽ được kích hoạt sau khi giáo vụ ghi nhận thanh toán đầu tiên.
                    </p>
                  )}

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

                  <div className="space-y-1.5 text-xs text-slate-500 mb-4">
                    {enr.expiresAt && (
                      <div>Hết hạn: {new Date(enr.expiresAt).toLocaleDateString('vi-VN')}</div>
                    )}
                    <div>Thời lượng mỗi buổi: {enr.sessionMinutes} phút</div>
                  </div>

                  <PaymentSummary enrollmentId={enr.id} />
                </div>

                {enr.status === 'ACTIVE' && enr.sessionsRemaining > 0 && (
                  <div className="pt-4 mt-4 border-t">
                    <Link
                      to="/student/book"
                      className="w-full inline-flex items-center justify-center py-2 px-3 rounded-xl bg-indigo-600 text-white font-semibold text-xs hover:bg-indigo-700 transition-colors shadow-xs"
                    >
                      Đặt lịch ngay
                    </Link>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
