import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { PAYMENT_NOTE_HINT, recordPayment, type PaymentMethod } from '@/lib/api/billing'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'

interface ManualPaymentDialogProps {
  enrollmentId: string
  debtVnd?: number
  open: boolean
  onClose: () => void
}

export default function ManualPaymentDialog({
  enrollmentId,
  debtVnd = 0,
  open,
  onClose,
}: ManualPaymentDialogProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  // Ngày theo giờ máy; toISOString theo UTC nên trước 7 giờ sáng sẽ ra ngày hôm qua
  const todayStr = new Date().toLocaleDateString('sv-SE')

  const [amountVnd, setAmountVnd] = useState<number>(debtVnd > 0 ? debtVnd : 0)
  const [paidAt, setPaidAt] = useState(todayStr)
  const [method, setMethod] = useState<PaymentMethod>('BANK_TRANSFER')
  const [note, setNote] = useState('')
  const [confirmOverpay, setConfirmOverpay] = useState(false)

  const paymentMutation = useMutation({
    mutationFn: () =>
      recordPayment(enrollmentId, {
        amountVnd: Number(amountVnd),
        paidAt,
        method,
        note: note.trim() || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrollment-statement', enrollmentId] })
      queryClient.invalidateQueries({ queryKey: ['enrollment', enrollmentId] })
      queryClient.invalidateQueries({ queryKey: ['enrollments'] })
      queryClient.invalidateQueries({ queryKey: ['receivables'] })
      toast.success('Ghi nhận thanh toán thành công! Gói học đã được cập nhật/kích hoạt.')
      onClose()
    },
    onError: (err) => showError(err),
    onSettled: () => setConfirmOverpay(false),
  })

  if (!open) return null

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (amountVnd <= 0) {
      toast.error('Số tiền thanh toán phải lớn hơn 0!')
      return
    }
    // Thu vượt được phép (spec) nhưng thường là gõ nhầm số: hỏi lại trước khi ghi
    if (amountVnd > Math.max(debtVnd, 0)) {
      setConfirmOverpay(true)
      return
    }
    paymentMutation.mutate()
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="p-5 border-b flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-bold text-slate-800">Ghi nhận thanh toán học phí</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100"
          >
            Đóng
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {debtVnd > 0 && (
            <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-xs flex justify-between items-center text-amber-900">
              <span>Công nợ hiện tại của ghi danh:</span>
              <strong className="text-sm font-bold text-amber-700">
                {debtVnd.toLocaleString('vi-VN')} đ
              </strong>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Số tiền thanh toán (VND) <span className="text-red-500">*</span>
            </label>
            <Input
              type="number"
              min={1000}
              step={1000}
              value={amountVnd}
              onChange={(e) => setAmountVnd(Number(e.target.value))}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Phương thức <span className="text-red-500">*</span>
              </label>
              <select
                value={method}
                onChange={(e) => setMethod(e.target.value as PaymentMethod)}
                className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
              >
                <option value="BANK_TRANSFER">Chuyển khoản</option>
                <option value="CASH">Tiền mặt</option>
                <option value="OTHER">Khác</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Ngày thanh toán <span className="text-red-500">*</span>
              </label>
              <Input
                type="date"
                max={todayStr}
                value={paidAt}
                onChange={(e) => setPaidAt(e.target.value)}
                required
              />
            </div>
          </div>

          <div>
            <label
              htmlFor="payment-note"
              className="block text-xs font-semibold text-slate-700 mb-1"
            >
              Ghi chú giao dịch
            </label>
            <Input
              id="payment-note"
              placeholder="Ví dụ: Mã GD VCB 88291, thu tiền mặt tại cơ sở…"
              maxLength={2000}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              aria-describedby="payment-note-help"
            />
            <p id="payment-note-help" className="text-xs text-slate-500 mt-1">
              {PAYMENT_NOTE_HINT}
            </p>
          </div>

          <div className="pt-4 border-t flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={paymentMutation.isPending}>
              {paymentMutation.isPending ? 'Đang lưu…' : 'Xác nhận thu học phí'}
            </Button>
          </div>
        </form>
      </div>

      <ConfirmDialog
        open={confirmOverpay}
        title="Số tiền vượt công nợ còn lại"
        description={
          <>
            Bạn đang ghi nhận <strong>{amountVnd.toLocaleString('vi-VN')} đ</strong>, trong khi công
            nợ còn lại là <strong>{Math.max(debtVnd, 0).toLocaleString('vi-VN')} đ</strong>. Phần
            chênh sẽ được ghi là thu vượt.
          </>
        }
        confirmLabel="Vẫn ghi nhận"
        cancelLabel="Sửa số tiền"
        busy={paymentMutation.isPending}
        onConfirm={() => paymentMutation.mutate()}
        onCancel={() => setConfirmOverpay(false)}
      />
    </div>
  )
}
