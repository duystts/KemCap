import { useState } from 'react'
import { useParams, Link } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '@/app/AuthProvider'
import {
  fetchEnrollment,
  changeTeacher,
  fetchTeacherHistory,
  extendEnrollment,
  cancelEnrollment,
  type TeacherAssignmentHistory,
} from '@/lib/api/catalog'
import { fetchUsers, type UserItem } from '@/lib/api/users'
import { fetchStatement, type PaymentItem } from '@/lib/api/billing'
import { StatusBadge } from '@/components/StatusBadge'
import ManualPaymentDialog from './ManualPaymentDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import {
  BookOpen,
  Calendar,
  CreditCard,
  History,
  AlertTriangle,
  RotateCw,
  Plus,
  X,
  Trash2,
} from 'lucide-react'

export default function EnrollmentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { me } = useAuth()
  const isAdmin = me?.role === 'ADMIN'
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  // State dialogs
  const [paymentDialogOpen, setPaymentDialogOpen] = useState(false)
  const [changeTeacherDialogOpen, setChangeTeacherDialogOpen] = useState(false)
  const [extendDialogOpen, setExtendDialogOpen] = useState(false)
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false)

  // Form states
  const [newTeacherId, setNewTeacherId] = useState('')
  const [changeTeacherReason, setChangeTeacherReason] = useState('')
  const [newExpiresAt, setNewExpiresAt] = useState('')
  const [cancelReason, setCancelReason] = useState('')

  // Queries
  const { data: enrollment, isLoading } = useQuery({
    queryKey: ['enrollment', id],
    queryFn: () => fetchEnrollment(id!),
    enabled: !!id,
  })

  const { data: statement } = useQuery({
    queryKey: ['enrollment-statement', id],
    queryFn: () => fetchStatement(id!),
    enabled: !!id,
  })

  const { data: teacherHistory = [] } = useQuery({
    queryKey: ['teacher-history', id],
    queryFn: () => fetchTeacherHistory(id!),
    enabled: !!id,
  })

  const { data: teachers = [] } = useQuery({
    queryKey: ['users', 'TEACHER'],
    queryFn: () => fetchUsers({ role: 'TEACHER', status: 'ACTIVE' }),
    enabled: changeTeacherDialogOpen,
  })

  // Mutations
  const changeTeacherMutation = useMutation({
    mutationFn: () =>
      changeTeacher(id!, {
        teacherId: newTeacherId,
        reason: changeTeacherReason.trim(),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrollment', id] })
      queryClient.invalidateQueries({ queryKey: ['teacher-history', id] })
      toast.success(
        'Đã đổi giáo viên thành công! Các buổi đã đặt với giáo viên cũ đã được hủy tự động.',
      )
      setChangeTeacherDialogOpen(false)
      setChangeTeacherReason('')
      setNewTeacherId('')
    },
    onError: (err) => showError(err),
  })

  const extendMutation = useMutation({
    mutationFn: () => extendEnrollment(id!, `${newExpiresAt}T23:59:59Z`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrollment', id] })
      toast.success('Gia hạn thời hạn gói học thành công!')
      setExtendDialogOpen(false)
    },
    onError: (err) => showError(err),
  })

  const cancelMutation = useMutation({
    mutationFn: () => cancelEnrollment(id!, cancelReason.trim()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrollment', id] })
      toast.success('Đã hủy gói học thành công!')
      setCancelDialogOpen(false)
    },
    onError: (err) => showError(err),
  })

  if (isLoading) {
    return (
      <div className="p-12 text-center text-sm text-slate-400">Đang tải chi tiết ghi danh…</div>
    )
  }

  if (!enrollment) {
    return (
      <div className="p-12 text-center text-sm text-slate-500">
        Không tìm thấy thông tin ghi danh này.
      </div>
    )
  }

  const formatVnd = (amount?: number | null) => {
    if (amount === undefined || amount === null) return '—'
    return amount.toLocaleString('vi-VN') + ' đ'
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Chi tiết ghi danh học tập
            </span>
            <StatusBadge status={enrollment.status} />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <BookOpen className="w-6 h-6 text-indigo-600" />
            Gói học ID: {enrollment.id.slice(0, 8)}
          </h1>
        </div>

        {isAdmin && (
          <div className="flex flex-wrap items-center gap-2">
            {enrollment.status !== 'CANCELLED' && (
              <>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setChangeTeacherDialogOpen(true)}
                >
                  <RotateCw className="w-4 h-4 mr-1.5" />
                  Đổi giáo viên
                </Button>
                <Button variant="outline" size="sm" onClick={() => setExtendDialogOpen(true)}>
                  <Calendar className="w-4 h-4 mr-1.5" />
                  Gia hạn gói
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setCancelDialogOpen(true)}
                  className="text-red-600 border-red-200 hover:bg-red-50"
                >
                  <Trash2 className="w-4 h-4 mr-1.5" />
                  Hủy gói
                </Button>
              </>
            )}
            {statement && statement.debtVnd > 0 && (
              <Button size="sm" onClick={() => setPaymentDialogOpen(true)}>
                <CreditCard className="w-4 h-4 mr-1.5" />
                Thu học phí
              </Button>
            )}
          </div>
        )}
      </div>

      {/* Counters Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs">
          <div className="text-xs font-semibold text-slate-500 uppercase">Còn lại khả dụng</div>
          <div className="text-3xl font-black text-indigo-600 mt-1">
            {enrollment.sessionsRemaining}
          </div>
          <div className="text-xs text-slate-400 mt-1">buổi học có thể đặt</div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs">
          <div className="text-xs font-semibold text-slate-500 uppercase">Đã học hoàn thành</div>
          <div className="text-3xl font-black text-emerald-600 mt-1">
            {enrollment.sessionsConsumed}
          </div>
          <div className="text-xs text-slate-400 mt-1">buổi đã diễn ra</div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs">
          <div className="text-xs font-semibold text-slate-500 uppercase">Đã đặt sắp tới</div>
          <div className="text-3xl font-black text-blue-600 mt-1">
            {enrollment.sessionsReserved}
          </div>
          <div className="text-xs text-slate-400 mt-1">buổi trong lịch</div>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-2xs">
          <div className="text-xs font-semibold text-slate-500 uppercase">Tổng số buổi</div>
          <div className="text-3xl font-black text-slate-900 mt-1">{enrollment.sessionsTotal}</div>
          <div className="text-xs text-slate-400 mt-1">{enrollment.sessionMinutes} phút / buổi</div>
        </div>
      </div>

      {/* Info Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Left: General & Timeline */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-bold text-slate-800">
              Thông tin gói & Thời hạn
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div className="flex justify-between py-2 border-b">
              <span className="text-slate-500">Mã học viên:</span>
              <Link
                to={`/admin/students/${enrollment.studentId}`}
                className="font-bold text-indigo-600 hover:underline"
              >
                {enrollment.studentId.slice(0, 8)}…
              </Link>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-slate-500">Giáo viên phụ trách:</span>
              <span className="font-semibold text-slate-800">
                {enrollment.teacherId.slice(0, 8)}…
              </span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-slate-500">Ngày kích hoạt:</span>
              <span className="font-semibold text-slate-800">
                {enrollment.activatedAt
                  ? new Date(enrollment.activatedAt).toLocaleDateString('vi-VN')
                  : 'Chưa kích hoạt (chờ thanh toán)'}
              </span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-slate-500">Ngày hết hạn gói:</span>
              <span className="font-semibold text-slate-800">
                {enrollment.expiresAt
                  ? new Date(enrollment.expiresAt).toLocaleDateString('vi-VN')
                  : 'Chưa xác định'}
              </span>
            </div>
            {enrollment.priceVnd !== null && (
              <div className="flex justify-between py-2">
                <span className="text-slate-500">Học phí áp dụng:</span>
                <span className="font-bold text-indigo-600">{formatVnd(enrollment.priceVnd)}</span>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Right: Statement & Payments */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <CardTitle className="text-base font-bold text-slate-800">
              Tình trạng công nợ & Thanh toán
            </CardTitle>
            {isAdmin && statement && statement.debtVnd > 0 && (
              <Button size="sm" variant="outline" onClick={() => setPaymentDialogOpen(true)}>
                <Plus className="w-3.5 h-3.5 mr-1" />
                Ghi nhận thu
              </Button>
            )}
          </CardHeader>
          <CardContent className="space-y-4">
            {statement ? (
              <div className="grid grid-cols-3 gap-2 p-3 bg-slate-50 rounded-xl border text-center">
                <div>
                  <div className="text-[11px] text-slate-500">Tổng phải thu</div>
                  <div className="font-bold text-sm text-slate-800 mt-0.5">
                    {formatVnd(statement.amountDueVnd)}
                  </div>
                </div>
                <div>
                  <div className="text-[11px] text-slate-500">Đã thanh toán</div>
                  <div className="font-bold text-sm text-emerald-600 mt-0.5">
                    {formatVnd(statement.amountPaidVnd)}
                  </div>
                </div>
                <div>
                  <div className="text-[11px] text-slate-500">Còn nợ</div>
                  <div className="font-bold text-sm text-amber-700 mt-0.5">
                    {formatVnd(statement.debtVnd)}
                  </div>
                </div>
              </div>
            ) : null}

            {/* Payments List */}
            <div>
              <div className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                Lịch sử thanh toán ({statement?.payments.length || 0})
              </div>
              {!statement || statement.payments.length === 0 ? (
                <div className="p-4 text-center text-xs text-slate-400 bg-slate-50 rounded-xl border border-dashed">
                  Chưa có giao dịch thanh toán nào được ghi nhận.
                </div>
              ) : (
                <div className="space-y-2">
                  {statement.payments.map((p: PaymentItem) => (
                    <div
                      key={p.id}
                      className="p-3 bg-white border rounded-xl text-xs flex justify-between items-center"
                    >
                      <div>
                        <div className="font-bold text-slate-800">{formatVnd(p.amountVnd)}</div>
                        <div className="text-[11px] text-slate-500 mt-0.5">
                          {p.paidAt} • {p.method === 'BANK_TRANSFER' ? 'Chuyển khoản' : p.method}
                          {p.note && ` • "${p.note}"`}
                        </div>
                      </div>
                      <span className="text-[11px] text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded font-medium">
                        Đã thu
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Teacher Assignment History */}
      {teacherHistory.length > 0 && (
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center gap-2">
              <History className="w-5 h-5 text-slate-500" />
              <CardTitle className="text-base font-bold text-slate-800">
                Lịch sử phân công giáo viên ({teacherHistory.length})
              </CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-500 font-semibold border-b">
                  <tr>
                    <th className="py-2.5 px-3">Thời gian</th>
                    <th className="py-2.5 px-3">Từ giáo viên</th>
                    <th className="py-2.5 px-3">Sang giáo viên</th>
                    <th className="py-2.5 px-3">Lý do điều chuyển</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {teacherHistory.map((h: TeacherAssignmentHistory, idx: number) => (
                    <tr key={idx}>
                      <td className="py-2.5 px-3 text-slate-600">
                        {new Date(h.changedAt).toLocaleString('vi-VN')}
                      </td>
                      <td className="py-2.5 px-3 text-slate-500">
                        {h.fromTeacherId ? `${h.fromTeacherId.slice(0, 8)}…` : '(Ban đầu)'}
                      </td>
                      <td className="py-2.5 px-3 font-semibold text-slate-800">
                        {h.toTeacherId.slice(0, 8)}…
                      </td>
                      <td className="py-2.5 px-3 text-slate-700">{h.reason}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Dialogs */}
      {/* 1. Thu học phí */}
      <ManualPaymentDialog
        enrollmentId={enrollment.id}
        debtVnd={statement?.debtVnd}
        open={paymentDialogOpen}
        onClose={() => setPaymentDialogOpen(false)}
      />

      {/* 2. Đổi giáo viên */}
      {changeTeacherDialogOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-2 border-b">
              <h3 className="text-base font-bold text-slate-800">Đổi giáo viên phụ trách</h3>
              <button
                type="button"
                onClick={() => setChangeTeacherDialogOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-xs text-amber-800 flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <strong>Lưu ý:</strong> Khi đổi giáo viên, các buổi học trong tương lai đã đặt với
                giáo viên cũ sẽ tự động bị hủy và hoàn lại số buổi khả dụng cho học viên.
              </div>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                if (!newTeacherId) {
                  toast.error('Vui lòng chọn giáo viên mới!')
                  return
                }
                if (!changeTeacherReason.trim()) {
                  toast.error('Vui lòng nhập lý do đổi giáo viên!')
                  return
                }
                changeTeacherMutation.mutate()
              }}
              className="space-y-3"
            >
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Chọn giáo viên mới <span className="text-red-500">*</span>
                </label>
                <select
                  value={newTeacherId}
                  onChange={(e) => setNewTeacherId(e.target.value)}
                  className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
                  required
                >
                  <option value="">-- Chọn giáo viên mới --</option>
                  {teachers
                    .filter((t: UserItem) => t.id !== enrollment.teacherId)
                    .map((t: UserItem) => (
                      <option key={t.id} value={t.id}>
                        {t.fullName} ({t.email})
                      </option>
                    ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Lý do thay đổi <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={3}
                  placeholder="Ví dụ: Giáo viên bận lịch dài hạn, học viên đề xuất đổi…"
                  value={changeTeacherReason}
                  onChange={(e) => setChangeTeacherReason(e.target.value)}
                  className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setChangeTeacherDialogOpen(false)}
                >
                  Hủy
                </Button>
                <Button type="submit" disabled={changeTeacherMutation.isPending}>
                  {changeTeacherMutation.isPending ? 'Đang cập nhật…' : 'Xác nhận đổi'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 3. Gia hạn gói */}
      {extendDialogOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-sm w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-2 border-b">
              <h3 className="text-base font-bold text-slate-800">Gia hạn thời hạn gói</h3>
              <button
                type="button"
                onClick={() => setExtendDialogOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                if (!newExpiresAt) {
                  toast.error('Vui lòng chọn ngày hết hạn mới!')
                  return
                }
                extendMutation.mutate()
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Ngày hết hạn mới <span className="text-red-500">*</span>
                </label>
                <Input
                  type="date"
                  value={newExpiresAt}
                  onChange={(e) => setNewExpiresAt(e.target.value)}
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button type="button" variant="outline" onClick={() => setExtendDialogOpen(false)}>
                  Hủy
                </Button>
                <Button type="submit" disabled={extendMutation.isPending}>
                  {extendMutation.isPending ? 'Đang gia hạn…' : 'Xác nhận gia hạn'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 4. Hủy gói */}
      {cancelDialogOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-sm w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-2 border-b">
              <h3 className="text-base font-bold text-slate-800 text-red-600">Hủy gói học</h3>
              <button
                type="button"
                onClick={() => setCancelDialogOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                cancelMutation.mutate()
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Lý do hủy gói
                </label>
                <textarea
                  rows={3}
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  placeholder="Học viên yêu cầu hoàn tiền hoặc dừng học…"
                  className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button type="button" variant="outline" onClick={() => setCancelDialogOpen(false)}>
                  Đóng
                </Button>
                <Button
                  type="submit"
                  disabled={cancelMutation.isPending}
                  className="bg-red-600 hover:bg-red-700 text-white"
                >
                  {cancelMutation.isPending ? 'Đang hủy…' : 'Xác nhận hủy gói'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
