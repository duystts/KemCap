import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { recordOutcome, type SessionStatus } from '@/lib/api/scheduling'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { CheckCircle, AlertTriangle, XCircle, Clock } from 'lucide-react'

interface SessionOutcomeDialogProps {
  sessionId: string
  open: boolean
  onClose: () => void
  onSuccess?: () => void
}

const OUTCOMES: {
  status: SessionStatus
  label: string
  description: string
  icon: typeof CheckCircle
  color: string
}[] = [
  {
    status: 'COMPLETED',
    label: 'Hoàn thành tốt',
    description: 'Buổi học diễn ra thành công theo đúng kế hoạch.',
    icon: CheckCircle,
    color: 'text-emerald-600 bg-emerald-50 border-emerald-200',
  },
  {
    status: 'STUDENT_NO_SHOW',
    label: 'Học viên vắng mặt',
    description: 'Học viên không vào lớp sau 15 phút. Buổi học vẫn bị tính trừ vào gói.',
    icon: AlertTriangle,
    color: 'text-amber-600 bg-amber-50 border-amber-200',
  },
  {
    status: 'TEACHER_ABSENT',
    label: 'Giáo viên vắng / sự cố',
    description: 'Giáo viên có việc đột xuất hoặc vắng. Buổi học sẽ được hoàn lại cho học viên.',
    icon: XCircle,
    color: 'text-rose-600 bg-rose-50 border-rose-200',
  },
  {
    status: 'INTERRUPTED',
    label: 'Gián đoạn kỹ thuật',
    description: 'Mất mạng, sự cố âm thanh nghiêm trọng. Chuyển giáo vụ xem xét bù buổi.',
    icon: Clock,
    color: 'text-indigo-600 bg-indigo-50 border-indigo-200',
  },
]

export default function SessionOutcomeDialog({
  sessionId,
  open,
  onClose,
  onSuccess,
}: SessionOutcomeDialogProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [selectedOutcome, setSelectedOutcome] = useState<SessionStatus>('COMPLETED')
  const [note, setNote] = useState('')

  const outcomeMutation = useMutation({
    mutationFn: () => recordOutcome(sessionId, selectedOutcome, note),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-sessions'] })
      queryClient.invalidateQueries({ queryKey: ['session', sessionId] })
      toast.success('Đã chốt kết quả buổi học thành công!')
      onSuccess?.()
      onClose()
    },
    onError: (err) => showError(err),
  })

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-150">
        <div>
          <h3 className="text-lg font-bold text-slate-900">Chốt kết quả buổi học</h3>
          <p className="text-xs text-slate-500 mt-1">
            Vui lòng xác nhận kết quả diễn ra của buổi học để hệ thống cập nhật trừ buổi hoặc hoàn
            buổi cho học viên.
          </p>
        </div>

        <div className="space-y-2.5">
          {OUTCOMES.map((item) => {
            const Icon = item.icon
            const isSelected = selectedOutcome === item.status
            return (
              <button
                key={item.status}
                type="button"
                onClick={() => setSelectedOutcome(item.status)}
                className={`w-full p-3 rounded-xl border text-left flex items-start gap-3 transition-all ${
                  isSelected
                    ? `${item.color} ring-2 ring-indigo-600/20 font-medium`
                    : 'bg-white border-slate-200 hover:bg-slate-50'
                }`}
              >
                <Icon className="w-5 h-5 shrink-0 mt-0.5" />
                <div>
                  <div className="text-sm font-semibold text-slate-900">{item.label}</div>
                  <div className="text-xs text-slate-500 mt-0.5">{item.description}</div>
                </div>
              </button>
            )
          })}
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-700 mb-1">
            Ghi chú thêm (Tùy chọn):
          </label>
          <Input
            type="text"
            placeholder="Ví dụ: Học viên vào muộn 10 phút, ôn thêm thì quá khứ đơn..."
            value={note}
            onChange={(e) => setNote(e.target.value)}
            className="text-sm"
          />
        </div>

        <div className="flex items-center justify-end gap-2 pt-2 border-t">
          <Button
            type="button"
            variant="ghost"
            onClick={onClose}
            disabled={outcomeMutation.isPending}
          >
            Hủy bỏ
          </Button>
          <Button
            type="button"
            onClick={() => outcomeMutation.mutate()}
            disabled={outcomeMutation.isPending}
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-medium"
          >
            {outcomeMutation.isPending ? 'Đang lưu…' : 'Xác nhận chốt kết quả'}
          </Button>
        </div>
      </div>
    </div>
  )
}
