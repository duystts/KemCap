import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '@/app/AuthProvider'
import { fetchSession, previewCancelSession, cancelSession } from '@/lib/api/scheduling'
import { StatusBadge } from '@/components/StatusBadge'
import SessionOutcomeDialog from './SessionOutcomeDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { X, Video, Clock, Calendar, AlertTriangle, ExternalLink, CheckCircle2 } from 'lucide-react'

interface SessionDetailDrawerProps {
  sessionId: string | null
  open: boolean
  onClose: () => void
}

export default function SessionDetailDrawer({
  sessionId,
  open,
  onClose,
}: SessionDetailDrawerProps) {
  const { me } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [confirmCancel, setConfirmCancel] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [outcomeDialogOpen, setOutcomeDialogOpen] = useState(false)

  const isTeacher = me?.role === 'TEACHER'
  const isAdmin = me?.role === 'ADMIN'

  const { data: session, isLoading } = useQuery({
    queryKey: ['session', sessionId],
    queryFn: () => fetchSession(sessionId!),
    enabled: !!sessionId && open,
  })

  const { data: cancelPreview, isLoading: loadingPreview } = useQuery({
    queryKey: ['cancel-preview', sessionId],
    queryFn: () => previewCancelSession(sessionId!),
    enabled: !!sessionId && confirmCancel,
  })

  const cancelMutation = useMutation({
    mutationFn: () => cancelSession(sessionId!, cancelReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-sessions'] })
      queryClient.invalidateQueries({ queryKey: ['session', sessionId] })
      toast.success('Đã hủy lịch buổi học thành công!')
      setConfirmCancel(false)
      onClose()
    },
    onError: (err) => showError(err),
  })

  if (!open || !sessionId) return null

  // Tính cửa sổ mở phòng: 10 phút trước giờ học -> 15 phút sau giờ học
  const now = Date.now()
  const startMs = session ? new Date(session.startsAt).getTime() : 0
  const endMs = session ? new Date(session.endsAt).getTime() : 0
  const isRoomOpen =
    session?.status === 'BOOKED' && now >= startMs - 10 * 60 * 1000 && now <= endMs + 15 * 60 * 1000

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
    <>
      <div className="fixed inset-0 z-40 bg-slate-900/30 backdrop-blur-xs flex justify-end">
        <div className="bg-white w-full max-w-md h-full shadow-2xl flex flex-col justify-between overflow-y-auto animate-in slide-in-from-right duration-200">
          {/* Header */}
          <div>
            <div className="p-4 sm:p-5 border-b flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  Chi tiết buổi học
                </span>
                {session && <StatusBadge status={session.status} />}
              </div>
              <button
                type="button"
                onClick={onClose}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Content */}
            <div className="p-5 space-y-6">
              {isLoading || !session ? (
                <div className="p-12 text-center text-sm text-slate-400">Đang tải thông tin…</div>
              ) : (
                <>
                  {/* Thời gian */}
                  <div className="p-4 rounded-xl bg-slate-50 border space-y-2">
                    <div className="flex items-center gap-2 text-slate-700 text-sm font-semibold">
                      <Calendar className="w-4 h-4 text-indigo-600" />
                      {formatDateTime(session.startsAt).date}
                    </div>
                    <div className="flex items-center gap-2 text-slate-900 text-xl font-bold font-mono">
                      <Clock className="w-5 h-5 text-indigo-600" />
                      {formatDateTime(session.startsAt).time} -{' '}
                      {formatDateTime(session.endsAt).time}
                    </div>
                  </div>

                  {/* Cảnh báo hủy (nếu có) */}
                  {session.cancelReason && (
                    <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-xs text-red-800 space-y-1">
                      <div className="font-semibold flex items-center gap-1.5">
                        <AlertTriangle className="w-4 h-4 text-red-600" />
                        Lý do hủy buổi:
                      </div>
                      <p>{session.cancelReason}</p>
                    </div>
                  )}

                  {/* Nút Vào lớp học WebRTC */}
                  {session.status === 'BOOKED' && (
                    <div className="space-y-2">
                      <Button
                        type="button"
                        onClick={() => navigate(`/sessions/${session.id}/classroom`)}
                        disabled={!isRoomOpen}
                        className={`w-full py-6 text-base font-semibold shadow-md flex items-center justify-center gap-2 ${
                          isRoomOpen
                            ? 'bg-emerald-600 hover:bg-emerald-700 text-white animate-pulse'
                            : 'bg-slate-100 text-slate-400 border border-slate-200 cursor-not-allowed'
                        }`}
                      >
                        <Video className="w-5 h-5" />
                        {isRoomOpen ? 'Vào phòng học ngay' : 'Phòng học chưa mở'}
                      </Button>
                      {!isRoomOpen && (
                        <p className="text-[11px] text-center text-slate-400 italic">
                          (Phòng học sẽ tự động mở trước giờ bắt đầu 10 phút và đóng sau 15 phút)
                        </p>
                      )}
                    </div>
                  )}

                  {/* Chốt kết quả (Cho GV & Admin) */}
                  {(isTeacher || isAdmin) &&
                    (session.status === 'BOOKED' || session.status === 'COMPLETED') && (
                      <div className="pt-2">
                        <Button
                          type="button"
                          variant="outline"
                          onClick={() => setOutcomeDialogOpen(true)}
                          className="w-full flex items-center justify-center gap-2 border-indigo-200 text-indigo-700 hover:bg-indigo-50"
                        >
                          <CheckCircle2 className="w-4 h-4 text-indigo-600" />
                          Chốt / Cập nhật kết quả buổi học
                        </Button>
                      </div>
                    )}

                  {/* Khu vực Hủy buổi học */}
                  {session.status === 'BOOKED' && (
                    <div className="pt-4 border-t space-y-3">
                      {!confirmCancel ? (
                        <Button
                          type="button"
                          variant="ghost"
                          onClick={() => setConfirmCancel(true)}
                          className="w-full text-rose-600 hover:bg-rose-50 hover:text-rose-700 text-sm"
                        >
                          Hủy buổi học này
                        </Button>
                      ) : (
                        <div className="p-4 rounded-xl border border-rose-200 bg-rose-50/50 space-y-3 animate-in fade-in">
                          <div className="text-xs font-semibold text-rose-900 flex items-center gap-1.5">
                            <AlertTriangle className="w-4 h-4 text-rose-600" />
                            Xác nhận hủy lịch học
                          </div>

                          {loadingPreview ? (
                            <p className="text-xs text-slate-400">Đang kiểm tra chính sách phạt…</p>
                          ) : (
                            <p className="text-xs text-rose-800 leading-relaxed font-medium">
                              {cancelPreview?.policyMessage ||
                                (cancelPreview?.willConsumeSession
                                  ? '⚠️ Hủy muộn (dưới 12 tiếng): Bạn vẫn sẽ bị tính 1 buổi học.'
                                  : '✅ Hủy đúng hạn (trên 12 tiếng): Bạn sẽ được hoàn lại 1 buổi vào gói.')}
                            </p>
                          )}

                          <Input
                            type="text"
                            placeholder="Nhập lý do hủy (tùy chọn)..."
                            value={cancelReason}
                            onChange={(e) => setCancelReason(e.target.value)}
                            className="bg-white text-xs h-9"
                          />

                          <div className="flex items-center gap-2 pt-1">
                            <Button
                              type="button"
                              size="sm"
                              variant="ghost"
                              onClick={() => setConfirmCancel(false)}
                              className="text-xs text-slate-600"
                            >
                              Không hủy
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              disabled={cancelMutation.isPending}
                              onClick={() => cancelMutation.mutate()}
                              className="bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold ml-auto"
                            >
                              {cancelMutation.isPending ? 'Đang xử lý…' : 'Đồng ý hủy'}
                            </Button>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </>
              )}
            </div>
          </div>

          {/* Footer: Link tới trang độc lập */}
          <div className="p-4 border-t bg-slate-50 flex items-center justify-between text-xs text-slate-500">
            <span>Mã buổi: {sessionId.slice(0, 8)}</span>
            <button
              type="button"
              onClick={() => {
                onClose()
                navigate(`/sessions/${sessionId}`)
              }}
              className="flex items-center gap-1 text-indigo-600 hover:underline font-medium"
            >
              Mở trang đầy đủ <ExternalLink className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Dialog chốt kết quả */}
      <SessionOutcomeDialog
        sessionId={sessionId}
        open={outcomeDialogOpen}
        onClose={() => setOutcomeDialogOpen(false)}
      />
    </>
  )
}
