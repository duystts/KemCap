import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { decideInterruption, type InterruptionDecision } from '@/lib/api/scheduling'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { useApiErrorToast } from '@/components/useApiErrorToast'

const CONFIRM: Record<InterruptionDecision, { title: string; description: string; label: string }> =
  {
    COUNT: {
      title: 'Tính buổi này là đã học?',
      description: 'Buổi chuyển sang Hoàn thành và trừ 1 buổi trong gói của học viên.',
      label: 'Tính là đã học',
    },
    MAKE_UP: {
      title: 'Xếp bù buổi này?',
      description: 'Buổi không bị trừ. Học viên đặt một buổi mới để học bù.',
      label: 'Xếp bù',
    },
  }

export function MadeUpNote() {
  return (
    <p className="p-4 rounded-xl border bg-slate-50 text-sm text-slate-700">
      Đã xếp bù: buổi này không bị trừ, học viên đặt buổi mới để học bù.
    </p>
  )
}

export default function InterruptionDecisionPanel({
  sessionId,
  decision,
}: {
  sessionId: string
  decision?: InterruptionDecision | null
}) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [pending, setPending] = useState<InterruptionDecision | null>(null)
  // Hiện ngay sau khi bấm, trước khi tải lại buổi học; backend lưu quyết định (T147)
  const [madeUp, setMadeUp] = useState(false)

  const decisionMutation = useMutation({
    mutationFn: (decision: InterruptionDecision) => decideInterruption(sessionId, decision),
    onSuccess: (_, decision) => {
      setPending(null)
      queryClient.invalidateQueries({ queryKey: ['session', sessionId] })
      queryClient.invalidateQueries({ queryKey: ['my-sessions'] })
      if (decision === 'MAKE_UP') setMadeUp(true)
      toast.success(decision === 'COUNT' ? 'Đã tính là đã học.' : 'Đã xếp bù, buổi không bị trừ.')
    },
    onError: (err) => {
      setPending(null)
      showError(err)
    },
  })

  if (madeUp || decision === 'MAKE_UP') return <MadeUpNote />
  if (decision) return null

  return (
    <div className="p-4 rounded-xl border bg-slate-50 flex flex-wrap items-center justify-between gap-3">
      <div>
        <div className="text-sm font-semibold text-slate-800">Buổi học bị gián đoạn</div>
        <div className="text-xs text-slate-500 mt-0.5">
          Giáo vụ quyết định tính là đã học hay xếp bù.
        </div>
      </div>
      <div className="flex items-center gap-2">
        <Button type="button" variant="outline" onClick={() => setPending('MAKE_UP')}>
          Xếp bù
        </Button>
        <Button type="button" onClick={() => setPending('COUNT')}>
          Tính là đã học
        </Button>
      </div>

      <ConfirmDialog
        open={pending !== null}
        title={pending ? CONFIRM[pending].title : ''}
        description={pending ? CONFIRM[pending].description : ''}
        confirmLabel={pending ? CONFIRM[pending].label : undefined}
        busy={decisionMutation.isPending}
        onConfirm={() => pending && decisionMutation.mutate(pending)}
        onCancel={() => setPending(null)}
      />
    </div>
  )
}
