import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '@/app/AuthProvider'
import { fetchEnrollments, bookSession, type Slot, type EnrollmentItem } from '@/lib/api/scheduling'
import SlotPicker from './SlotPicker'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { Check, AlertCircle, BookOpen } from 'lucide-react'

export default function BookSessionPage() {
  const { me } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [selectedEnrollmentId, setSelectedEnrollmentId] = useState<string>('')
  const [selectedSlot, setSelectedSlot] = useState<Slot | null>(null)

  // 1. Lấy danh sách gói học active của học viên
  const { data: enrollments = [], isLoading: loadingEnrollments } = useQuery({
    queryKey: ['my-enrollments', me?.id],
    queryFn: () => fetchEnrollments({ studentId: me?.id, status: 'ACTIVE' }),
    enabled: !!me?.id,
  })

  // Chọn gói đầu tiên nếu chưa chọn
  const activeEnrollments = enrollments.filter((e) => e.status === 'ACTIVE')
  const currentEnrollment: EnrollmentItem | undefined =
    activeEnrollments.find((e) => e.id === selectedEnrollmentId) || activeEnrollments[0]

  const activeId = currentEnrollment?.id || ''

  // 2. Mutation đặt lịch
  const bookMutation = useMutation({
    mutationFn: (slot: Slot) =>
      bookSession({
        enrollmentId: activeId,
        startsAt: slot.startsAt,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-enrollments'] })
      queryClient.invalidateQueries({ queryKey: ['my-sessions'] })
      toast.success('Đặt lịch học thành công! Buổi học đã được lưu vào lịch của bạn.')
      setSelectedSlot(null)
      navigate('/student/schedule')
    },
    onError: (err: any) => {
      const code = err?.code
      if (code === 'SLOT_TAKEN') {
        toast.error('Khung giờ này vừa có người đặt. Bạn vui lòng chọn khung giờ khác nhé!')
      } else if (code === 'NO_SESSIONS_LEFT') {
        toast.error('Gói học của bạn đã hết số buổi khả dụng!')
      } else if (code === 'ENROLLMENT_NOT_ACTIVE') {
        toast.error('Gói học của bạn chưa kích hoạt hoặc đang chờ thanh toán!')
      } else {
        showError(err)
      }
    },
  })

  const formatSlotDisplay = (slot: Slot) => {
    const start = new Date(slot.startsAt)
    const end = new Date(slot.endsAt)
    const dateStr = start.toLocaleDateString('vi-VN', {
      weekday: 'long',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
    const timeStr = `${start.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
    })} - ${end.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`
    return { dateStr, timeStr }
  }

  return (
    <div className="space-y-8 max-w-4xl mx-auto p-4 sm:p-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">Đặt lịch học 1 kèm 1</h1>
        <p className="text-sm text-slate-500 mt-1">
          Chọn gói học của bạn, tìm khung giờ rảnh phù hợp với giáo viên và xác nhận đặt buổi.
        </p>
      </div>

      {loadingEnrollments ? (
        <div className="p-8 text-center text-sm text-slate-400">Đang tải gói học của bạn…</div>
      ) : activeEnrollments.length === 0 ? (
        <Card className="border-amber-200 bg-amber-50/50">
          <CardContent className="p-6 text-center space-y-3">
            <AlertCircle className="w-8 h-8 text-amber-600 mx-auto" />
            <h3 className="font-semibold text-slate-800">Bạn chưa có gói học nào khả dụng</h3>
            <p className="text-sm text-slate-600 max-w-md mx-auto">
              Để đặt lịch học, bạn cần có ít nhất một gói học đang trong trạng thái kích hoạt và còn
              buổi học. Vui lòng liên hệ giáo vụ để được hỗ trợ!
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-6">
          {/* Bước 1: Chọn gói học */}
          <Card>
            <CardHeader className="pb-3 border-b">
              <CardTitle className="text-base flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-indigo-600" />
                Gói học áp dụng
              </CardTitle>
            </CardHeader>
            <CardContent className="pt-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {activeEnrollments.map((enr) => {
                  const isSelected = (selectedEnrollmentId || activeEnrollments[0].id) === enr.id
                  return (
                    <button
                      key={enr.id}
                      type="button"
                      onClick={() => {
                        setSelectedEnrollmentId(enr.id)
                        setSelectedSlot(null)
                      }}
                      className={`p-4 rounded-xl border text-left transition-all ${
                        isSelected
                          ? 'border-indigo-600 bg-indigo-50/50 ring-2 ring-indigo-600/20'
                          : 'border-slate-200 bg-white hover:border-slate-300'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-slate-900 text-sm">
                          Gói {enr.sessionMinutes} phút/buổi
                        </span>
                        {isSelected && <Check className="w-4 h-4 text-indigo-600 font-bold" />}
                      </div>
                      <div className="mt-2 flex items-center justify-between text-xs text-slate-600">
                        <span>
                          Còn lại:{' '}
                          <strong className="text-indigo-600 font-semibold">
                            {enr.sessionsRemaining}
                          </strong>{' '}
                          / {enr.sessionsTotal} buổi
                        </span>
                        <span>Đã học: {enr.sessionsConsumed}</span>
                      </div>
                      {enr.expiresAt && (
                        <div className="text-[11px] text-slate-400 mt-1">
                          Hạn dùng: {new Date(enr.expiresAt).toLocaleDateString('vi-VN')}
                        </div>
                      )}
                    </button>
                  )
                })}
              </div>
            </CardContent>
          </Card>

          {/* Bước 2: Chọn khung giờ trống */}
          {activeId && (
            <Card>
              <CardContent className="pt-6">
                <SlotPicker
                  enrollmentId={activeId}
                  selectedSlot={selectedSlot}
                  onSelectSlot={(slot) => setSelectedSlot(slot)}
                />
              </CardContent>
            </Card>
          )}

          {/* Bước 3: Xác nhận đặt buổi */}
          {selectedSlot && (
            <div className="fixed bottom-0 left-0 right-0 sm:static p-4 sm:p-0 bg-white sm:bg-transparent border-t sm:border-0 shadow-lg sm:shadow-none z-20">
              <div className="p-4 rounded-2xl bg-indigo-900 text-white flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-xl">
                <div>
                  <div className="text-xs text-indigo-300 uppercase tracking-wider font-semibold">
                    Khung giờ đã chọn
                  </div>
                  <div className="text-base font-bold mt-0.5">
                    {formatSlotDisplay(selectedSlot).dateStr}
                  </div>
                  <div className="text-sm text-indigo-200">
                    Thời gian: {formatSlotDisplay(selectedSlot).timeStr} (
                    {selectedSlot.durationMinutes} phút)
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setSelectedSlot(null)}
                    className="text-indigo-200 hover:text-white hover:bg-indigo-800"
                  >
                    Hủy chọn
                  </Button>
                  <Button
                    type="button"
                    disabled={bookMutation.isPending}
                    onClick={() => bookMutation.mutate(selectedSlot)}
                    className="bg-emerald-500 hover:bg-emerald-600 text-white font-semibold px-6 shadow-md"
                  >
                    {bookMutation.isPending ? 'Đang đặt lịch…' : 'Xác nhận Đặt buổi này'}
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
