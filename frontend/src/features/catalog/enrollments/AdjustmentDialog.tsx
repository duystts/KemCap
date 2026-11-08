import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { PAYMENT_NOTE_HINT, recordAdjustment } from '@/lib/api/billing'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'

interface AdjustmentDialogProps {
  enrollmentId: string
  amountPaidVnd: number
  onClose: () => void
}

/** Điều chỉnh khoản nhập nhầm: nhập số tiền giảm (dương), gửi lên backend dưới dạng số âm. */
export default function AdjustmentDialog({
  enrollmentId,
  amountPaidVnd,
  onClose,
}: AdjustmentDialogProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')

  const value = Number(amount)
  const amountError =
    amount === ''
      ? null
      : !Number.isInteger(value) || value <= 0
        ? 'Số tiền giảm phải là số nguyên lớn hơn 0.'
        : value > amountPaidVnd
          ? `Không được giảm quá tổng đã thu (${amountPaidVnd.toLocaleString('vi-VN')} đ).`
          : null

  const mutation = useMutation({
    mutationFn: () => recordAdjustment(enrollmentId, { amountVnd: -value, note: note.trim() }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrollment-statement', enrollmentId] })
      queryClient.invalidateQueries({ queryKey: ['receivables'] })
      toast.success('Đã ghi điều chỉnh.')
      onClose()
    },
    onError: (err) => showError(err),
  })

  const canSubmit = amount !== '' && !amountError && note.trim() !== '' && !mutation.isPending

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 flex items-center justify-center p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="adjustment-title"
        className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden"
      >
        <div className="p-5 border-b">
          <h2 id="adjustment-title" className="text-lg font-bold text-slate-800">
            Điều chỉnh khoản thu
          </h2>
          <p className="text-xs text-slate-500 mt-1">
            Dùng khi đã ghi nhầm số tiền. Khoản điều chỉnh được lưu riêng trong lịch sử, không xóa
            khoản cũ.
          </p>
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (canSubmit) mutation.mutate()
          }}
          className="p-6 space-y-4"
        >
          <div>
            <label
              htmlFor="adjustment-amount"
              className="block text-xs font-semibold text-slate-700 mb-1"
            >
              Số tiền giảm (VND)
            </label>
            <Input
              id="adjustment-amount"
              type="number"
              inputMode="numeric"
              min={1}
              max={amountPaidVnd}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              aria-invalid={amountError ? true : undefined}
              aria-describedby="adjustment-amount-help"
              required
            />
            <p
              id="adjustment-amount-help"
              className={`text-xs mt-1 ${amountError ? 'text-red-600' : 'text-slate-500'}`}
            >
              {amountError ?? `Tổng đã thu hiện tại: ${amountPaidVnd.toLocaleString('vi-VN')} đ.`}
            </p>
          </div>

          <div>
            <label
              htmlFor="adjustment-note"
              className="block text-xs font-semibold text-slate-700 mb-1"
            >
              Lý do điều chỉnh
            </label>
            <textarea
              id="adjustment-note"
              rows={3}
              maxLength={2000}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Ví dụ: Nhập nhầm 1.500.000 thay vì 150.000 ở khoản thu ngày 05/11"
              aria-describedby="adjustment-note-help"
              className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
              required
            />
            <p id="adjustment-note-help" className="text-xs text-slate-500 mt-1">
              {PAYMENT_NOTE_HINT}
            </p>
          </div>

          <div className="pt-4 border-t flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={!canSubmit}>
              {mutation.isPending ? 'Đang lưu…' : 'Ghi điều chỉnh'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
