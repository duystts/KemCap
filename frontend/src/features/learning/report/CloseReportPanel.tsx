import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { closeSessionReport } from '@/lib/api/learning'
import { Button } from '@/components/ui/button'
import { useApiErrorToast } from '@/components/useApiErrorToast'

/**
 * Giáo vụ đóng phiếu treo khi giáo viên của buổi không còn phụ trách học viên, ví dụ đã nghỉ việc
 * (T148). Phiếu đóng không còn bị nhắc quá hạn.
 */
export default function CloseReportPanel({ sessionId }: { sessionId: string }) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')

  const closeMutation = useMutation({
    mutationFn: () => closeSessionReport(sessionId, reason.trim()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['session-report', sessionId] })
      queryClient.invalidateQueries({ queryKey: ['session-reports'] })
      toast.success('Đã đóng phiếu.')
    },
    onError: showError,
  })

  if (!open) {
    return (
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-slate-600">
          Giáo viên của buổi không còn phụ trách học viên. Nếu giáo viên không nộp được, ví dụ đã
          nghỉ việc, giáo vụ đóng phiếu để hết nhắc.
        </p>
        <Button type="button" variant="outline" onClick={() => setOpen(true)}>
          Đóng phiếu
        </Button>
      </div>
    )
  }

  return (
    <form
      className="space-y-2"
      onSubmit={(e) => {
        e.preventDefault()
        if (reason.trim()) closeMutation.mutate()
      }}
    >
      <label htmlFor="close-report-reason" className="block text-xs font-semibold text-slate-700">
        Lý do đóng phiếu
      </label>
      <textarea
        id="close-report-reason"
        rows={2}
        maxLength={2000}
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        placeholder="Ví dụ: Giáo viên đã nghỉ việc, không nộp được phiếu"
        className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
        required
      />
      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={() => setOpen(false)}>
          Thôi
        </Button>
        <Button type="submit" disabled={!reason.trim() || closeMutation.isPending}>
          Xác nhận đóng phiếu
        </Button>
      </div>
    </form>
  )
}
