import { useState } from 'react'
import { useParams, useNavigate } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '@/app/AuthProvider'
import { fetchSession, previewCancelSession, cancelSession } from '@/lib/api/scheduling'
import { StatusBadge } from '@/components/StatusBadge'
import InterruptionDecisionPanel, { MadeUpNote } from './InterruptionDecisionPanel'
import RescheduleSessionPanel from './RescheduleSessionPanel'
import SessionOutcomeDialog from './SessionOutcomeDialog'
import SessionReportSection from '@/features/learning/report/SessionReportSection'
import SessionFeedbackPrompt from '@/features/learning/feedback/SessionFeedbackPrompt'
import SessionFeedbackView from '@/features/learning/feedback/SessionFeedbackView'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'

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
    // Giáo viên không có quyền xem trước: hủy của giáo viên không tính vào gói học viên
    enabled: !!id && session?.status === 'BOOKED' && me?.role !== 'TEACHER',
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
          Quay lại
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
                Ngày học
              </span>
              <p className="text-base font-semibold text-slate-800 mt-1">
                {formatDateTime(session.startsAt).date}
              </p>
            </div>
            <div className="p-4 rounded-xl bg-slate-50 border">
              <span className="text-xs text-slate-500 flex items-center gap-1.5 font-medium">
                Khung giờ (Múi giờ của bạn)
              </span>
              <p className="text-base font-semibold text-slate-800 mt-1">
                {formatDateTime(session.startsAt).time} - {formatDateTime(session.endsAt).time}
              </p>
            </div>
          </div>

          {/* Cảnh báo hủy nếu có */}
          {session.cancelReason && (
            <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-sm text-red-800 space-y-1">
              <div className="font-semibold flex items-center gap-1.5">Lý do hủy buổi:</div>
              <p>{session.cancelReason}</p>
            </div>
          )}

          {/* Nút Vào phòng học WebRTC */}
          {session.status === 'BOOKED' && (
            <div className="p-5 rounded-xl border bg-slate-50 space-y-4">
              <div>
                <h3 className="text-base font-semibold text-slate-900">Phòng học trực tuyến</h3>
                <p className="text-xs text-slate-600 mt-1 leading-relaxed">
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
                className={`w-full h-11 text-sm font-semibold ${
                  isRoomOpen
                    ? 'bg-indigo-600 hover:bg-indigo-700 text-white'
                    : 'bg-slate-100 text-slate-500 border cursor-not-allowed'
                }`}
              >
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
                  Chốt kết quả
                </Button>
              </div>
            )}

          {/* Gián đoạn: giáo vụ quyết định */}
          {isAdmin && session.status === 'INTERRUPTED' && (
            <InterruptionDecisionPanel
              sessionId={session.id}
              decision={session.interruptionDecision}
            />
          )}
          {!isAdmin && session.interruptionDecision === 'MAKE_UP' && <MadeUpNote />}

          {/* Đổi lịch: ẩn sau hạn chót */}
          {session.status === 'BOOKED' &&
            cancelPreview &&
            (cancelPreview.willConsume ? (
              <p className="text-xs text-slate-500">
                Đã quá hạn đổi lịch. Bạn vẫn có thể hủy muộn và bị tính 1 buổi.
              </p>
            ) : (
              <RescheduleSessionPanel session={session} />
            ))}

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
                    Xác nhận hủy buổi học
                  </div>
                  {loadingPreview ? (
                    <p className="text-xs text-slate-400">Đang kiểm tra chính sách phạt…</p>
                  ) : (
                    <p className="text-xs text-rose-800 font-medium">
                      {me?.role === 'TEACHER'
                        ? 'Buổi sẽ được ghi nhận là giáo viên vắng.'
                        : cancelPreview?.willConsume
                          ? 'Hủy muộn (dưới 12 tiếng): Bạn vẫn sẽ bị tính trừ 1 buổi học.'
                          : 'Hủy đúng hạn (trên 12 tiếng): Bạn sẽ được hoàn lại 1 buổi vào gói.'}
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

      {session.status === 'COMPLETED' && <SessionReportSection sessionId={session.id} />}
      {me?.role === 'STUDENT' && <SessionFeedbackPrompt session={session} />}
      {isAdmin && session.status === 'COMPLETED' && <SessionFeedbackView sessionId={session.id} />}

      <SessionOutcomeDialog
        sessionId={session.id}
        open={outcomeDialogOpen}
        onClose={() => setOutcomeDialogOpen(false)}
      />
    </div>
  )
}
