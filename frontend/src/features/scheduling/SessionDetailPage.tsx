import { useState } from 'react'
import { useParams, useNavigate } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '@/app/AuthProvider'
import { fetchSession, previewCancelSession, cancelSession } from '@/lib/api/scheduling'
import { StatusBadge } from '@/components/StatusBadge'
import SessionOutcomeDialog from './SessionOutcomeDialog'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { ChevronLeft, Calendar, Clock, Video, AlertTriangle, CheckCircle2 } from 'lucide-react'

export default function SessionDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { me } = useAuth()
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [confirmCancel, setConfirmCancel] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [outcomeDialogOpen, setOutcomeDialogOpen] = useState(false)

  const isTeacher = me?.role === 'TEACHER'
  const isAdmin = me?.role === 'ADMIN'

  const {
    data: session,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['session', id],
    queryFn: () => fetchSession(id!),
    enabled: !!id,
  })

  const { data: cancelPreview, isLoading: loadingPreview } = useQuery({
    queryKey: ['cancel-preview', id],
    queryFn: () => previewCancelSession(id!),
    enabled: !!id && confirmCancel,
  })

  const cancelMutation = useMutation({
    mutationFn: () => cancelSession(id!, cancelReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['session', id] })
      queryClient.invalidateQueries({ queryKey: ['my-sessions'] })
      toast.success('Đã hủy lịch buổi học thành công!')
      setConfirmCancel(false)
    },
    onError: (err) => showError(err),
  })

  if (isLoading) {
    return (
      <div className="p-12 text-center text-sm text-slate-400">Đang tải thông tin buổi học…</div>
    )
  }

  if (error || !session) {
    return (
      <div className="max-w-2xl mx-auto p-8 text-center space-y-4">
        <AlertTriangle className="w-10 h-10 text-amber-500 mx-auto" />
        <h2 className="text-lg font-bold text-slate-800">Không tìm thấy thông tin buổi học</h2>
        <Button onClick={() => navigate(-1)} variant="outline">
          Quay lại
        </Button>
      </div>
    )
  }

  const now = Date.now()
  const startMs = new Date(session.startsAt).getTime()
  const endMs = new Date(session.endsAt).getTime()
  const isRoomOpen =
    session.status === 'BOOKED' && now >= startMs - 10 * 60 * 1000 && now <= endMs + 15 * 60 * 1000

  const formatDateTime = (iso: string) => {
    const d = new Date(iso)
    return {
      date: d.toLocaleDateString('vi-VN', {
        weekday: 'long',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      }),
      time: d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    }
  }

  return (
    <div className="max-w-3xl mx-auto p-4 sm:p-6 space-y-6">
      <div className="flex items-center gap-3">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => navigate(-1)}
          className="flex items-center gap-1 text-slate-600 hover:text-slate-900"
        >
          <ChevronLeft className="w-4 h-4" /> Quay lại
        </Button>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-4 border-b">
          <div>
            <CardTitle className="text-xl font-bold text-slate-900">
              Chi tiết Buổi học 1-1
            </CardTitle>
            <p className="text-xs text-slate-400 mt-0.5">Mã phiên: {session.id}</p>
          </div>
          <StatusBadge status={session.status} />
        </CardHeader>
        <CardContent className="pt-6 space-y-6">
          {/* Thông tin thời gian */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-4 rounded-xl bg-slate-50 border">
              <span className="text-xs text-slate-500 flex items-center gap-1.5 font-medium">
                <Calendar className="w-4 h-4 text-indigo-600" /> Ngày học
              </span>
              <p className="text-base font-semibold text-slate-800 mt-1">
                {formatDateTime(session.startsAt).date}
              </p>
            </div>
            <div className="p-4 rounded-xl bg-slate-50 border">
              <span className="text-xs text-slate-500 flex items-center gap-1.5 font-medium">
                <Clock className="w-4 h-4 text-indigo-600" /> Khung giờ (Múi giờ của bạn)
              </span>
              <p className="text-base font-bold text-indigo-600 font-mono mt-1">
                {formatDateTime(session.startsAt).time} - {formatDateTime(session.endsAt).time}
              </p>
            </div>
          </div>

          {/* Cảnh báo hủy nếu có */}
          {session.cancelReason && (
            <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-sm text-red-800 space-y-1">
              <div className="font-semibold flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4 text-red-600" /> Lý do hủy buổi:
              </div>
              <p>{session.cancelReason}</p>
            </div>
          )}

          {/* Nút Vào phòng học WebRTC */}
          {session.status === 'BOOKED' && (
            <div className="p-6 rounded-2xl bg-gradient-to-r from-indigo-900 to-indigo-950 text-white space-y-4 shadow-xl">
              <div>
                <h3 className="font-bold text-lg flex items-center gap-2">
                  <Video className="w-5 h-5 text-emerald-400" />
                  Phòng học trực tuyến LiveKit WebRTC
                </h3>
                <p className="text-xs text-indigo-200 mt-1 leading-relaxed">
                  Phòng học tích hợp video HD, âm thanh 2 chiều, bảng chat và chia sẻ màn hình.
                  {isRoomOpen
                    ? ' Phòng học đã mở, bạn có thể tham gia ngay!'
                    : ' Phòng học sẽ mở tự động trước giờ học 10 phút.'}
                </p>
              </div>

              <Button
                type="button"
                onClick={() => navigate(`/sessions/${session.id}/classroom`)}
                disabled={!isRoomOpen}
                className={`w-full py-6 text-base font-semibold shadow-md flex items-center justify-center gap-2 ${
                  isRoomOpen
                    ? 'bg-emerald-500 hover:bg-emerald-600 text-white animate-pulse'
                    : 'bg-indigo-800/60 text-indigo-300 border border-indigo-700/50 cursor-not-allowed'
                }`}
              >
                <Video className="w-5 h-5" />
                {isRoomOpen ? 'Tham gia lớp học ngay' : 'Phòng học chưa tới giờ mở'}
              </Button>
            </div>
          )}

          {/* Chốt kết quả (Giáo viên / Admin) */}
          {(isTeacher || isAdmin) &&
            (session.status === 'BOOKED' || session.status === 'COMPLETED') && (
              <div className="p-4 rounded-xl border border-indigo-100 bg-indigo-50/30 flex items-center justify-between">
                <div>
                  <div className="text-sm font-semibold text-slate-800">
                    Chốt hoặc cập nhật kết quả buổi học
                  </div>
                  <div className="text-xs text-slate-500 mt-0.5">
                    Xác nhận hoàn thành, học viên vắng mặt hoặc sự cố kỹ thuật.
                  </div>
                </div>
                <Button
                  type="button"
                  onClick={() => setOutcomeDialogOpen(true)}
                  className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm"
                >
                  <CheckCircle2 className="w-4 h-4 mr-1.5" /> Chốt kết quả
                </Button>
              </div>
            )}

          {/* Hủy buổi học */}
          {session.status === 'BOOKED' && (
            <div className="pt-4 border-t space-y-4">
              {!confirmCancel ? (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setConfirmCancel(true)}
                  className="border-rose-200 text-rose-600 hover:bg-rose-50"
                >
                  Hủy lịch buổi học này
                </Button>
              ) : (
                <div className="p-4 rounded-xl border border-rose-200 bg-rose-50 space-y-3">
                  <div className="text-sm font-bold text-rose-900 flex items-center gap-2">
                    <AlertTriangle className="w-5 h-5 text-rose-600" />
                    Xác nhận hủy buổi học
                  </div>
                  {loadingPreview ? (
                    <p className="text-xs text-slate-400">Đang kiểm tra chính sách phạt…</p>
                  ) : (
                    <p className="text-xs text-rose-800 font-medium">
                      {cancelPreview?.policyMessage ||
                        (cancelPreview?.willConsumeSession
                          ? '⚠️ Hủy muộn (dưới 12 tiếng): Bạn vẫn sẽ bị tính trừ 1 buổi học.'
                          : '✅ Hủy đúng hạn (trên 12 tiếng): Bạn sẽ được hoàn lại 1 buổi vào gói.')}
                    </p>
                  )}
                  <Input
                    type="text"
                    placeholder="Lý do hủy buổi học..."
                    value={cancelReason}
                    onChange={(e) => setCancelReason(e.target.value)}
                    className="bg-white text-sm"
                  />
                  <div className="flex items-center gap-2 pt-2">
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={() => setConfirmCancel(false)}
                      className="text-xs text-slate-600"
                    >
                      Giữ lại buổi học
                    </Button>
                    <Button
                      type="button"
                      disabled={cancelMutation.isPending}
                      onClick={() => cancelMutation.mutate()}
                      className="bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold ml-auto"
                    >
                      {cancelMutation.isPending ? 'Đang hủy…' : 'Xác nhận hủy'}
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <SessionOutcomeDialog
        sessionId={session.id}
        open={outcomeDialogOpen}
        onClose={() => setOutcomeDialogOpen(false)}
      />
    </div>
  )
}
