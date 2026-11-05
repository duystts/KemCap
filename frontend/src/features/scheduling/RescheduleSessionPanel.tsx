import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { rescheduleSession, type SessionItem, type Slot } from '@/lib/api/scheduling'
import { ApiError } from '@/lib/api/client'
import { Button } from '@/components/ui/button'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import SlotPicker from './SlotPicker'

/** Đổi lịch: backend hủy đúng hạn buổi cũ và đặt buổi mới trong một transaction. */
export default function RescheduleSessionPanel({ session }: { session: SessionItem }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [open, setOpen] = useState(false)
  const [slot, setSlot] = useState<Slot | null>(null)

  const rescheduleMutation = useMutation({
    mutationFn: (newStartsAt: string) => rescheduleSession(session.id, newStartsAt),
    onSuccess: (next) => {
      queryClient.invalidateQueries({ queryKey: ['session', session.id] })
      queryClient.invalidateQueries({ queryKey: ['my-sessions'] })
      queryClient.invalidateQueries({ queryKey: ['available-slots', session.enrollmentId] })
      toast.success('Đã đổi lịch buổi học!')
      navigate(`/sessions/${next.id}`, { replace: true })
    },
    onError: (err) => {
      showError(err)
      if (err instanceof ApiError && err.code === 'SLOT_TAKEN') {
        setSlot(null)
        queryClient.invalidateQueries({ queryKey: ['available-slots', session.enrollmentId] })
      }
      if (err instanceof ApiError && err.code === 'CANCEL_WINDOW_PASSED') {
        setOpen(false)
        queryClient.invalidateQueries({ queryKey: ['cancel-preview', session.id] })
      }
    },
  })

  if (!open) {
    return (
      <Button type="button" variant="outline" onClick={() => setOpen(true)}>
        Đổi lịch
      </Button>
    )
  }

  return (
    <div className="p-4 rounded-xl border space-y-4">
      <h3 className="text-sm font-semibold text-slate-900">Đổi lịch buổi học</h3>
      <SlotPicker enrollmentId={session.enrollmentId} selectedSlot={slot} onSelectSlot={setSlot} />
      <div className="flex items-center justify-end gap-2 pt-2 border-t">
        <Button type="button" variant="ghost" onClick={() => setOpen(false)}>
          Giữ lịch cũ
        </Button>
        <Button
          type="button"
          disabled={!slot || rescheduleMutation.isPending}
          onClick={() => slot && rescheduleMutation.mutate(slot.startsAt)}
        >
          {rescheduleMutation.isPending ? 'Đang đổi…' : 'Xác nhận đổi lịch'}
        </Button>
      </div>
    </div>
  )
}
