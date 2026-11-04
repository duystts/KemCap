import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { saveGuardianConsent, type GuardianConsentData } from '@/lib/api/users'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { X, Shield, CheckCircle } from 'lucide-react'

interface GuardianConsentDialogProps {
  studentId: string | null
  studentName?: string
  open: boolean
  onClose: () => void
}

export default function GuardianConsentDialog({
  studentId,
  studentName,
  open,
  onClose,
}: GuardianConsentDialogProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const todayStr = new Date().toISOString().slice(0, 10)

  const [guardianName, setGuardianName] = useState('')
  const [relationship, setRelationship] = useState('Cha / Mẹ')
  const [guardianContact, setGuardianContact] = useState('')
  const [consentedOn, setConsentedOn] = useState(todayStr)

  const consentMutation = useMutation({
    mutationFn: (data: GuardianConsentData) => saveGuardianConsent(studentId!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      queryClient.invalidateQueries({ queryKey: ['students'] })
      toast.success(
        'Đã ghi nhận đồng ý của người giám hộ! Hệ thống đã gửi thư mời kích hoạt tài khoản cho học viên.',
      )
      onClose()
    },
    onError: (err) => showError(err),
  })

  if (!open || !studentId) return null

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!guardianName.trim() || !guardianContact.trim()) {
      toast.error('Vui lòng điền họ tên và thông tin liên hệ của người giám hộ!')
      return
    }

    consentMutation.mutate({
      guardianName: guardianName.trim(),
      relationship,
      guardianContact: guardianContact.trim(),
      consentedOn,
    })
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="p-5 border-b flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Shield className="w-5 h-5 text-amber-600" />
            <h2 className="text-lg font-bold text-slate-800">Đồng ý của người giám hộ</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="p-3 bg-slate-50 border rounded-xl text-xs text-slate-600">
            Học viên: <strong className="text-slate-800">{studentName || 'Học viên'}</strong> (dưới
            16 tuổi). Theo quy định bảo vệ dữ liệu cá nhân (FR-030), cần có sự đồng ý của phụ huynh
            để kích hoạt tài khoản.
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Họ và tên người giám hộ <span className="text-red-500">*</span>
            </label>
            <Input
              type="text"
              placeholder="Ví dụ: Hoàng Thị Lan"
              value={guardianName}
              onChange={(e) => setGuardianName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Mối quan hệ <span className="text-red-500">*</span>
            </label>
            <select
              value={relationship}
              onChange={(e) => setRelationship(e.target.value)}
              className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
            >
              <option value="Cha / Mẹ">Cha / Mẹ</option>
              <option value="Người giám hộ hợp pháp">Người giám hộ hợp pháp</option>
              <option value="Khác">Khác</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Điện thoại / Email người giám hộ <span className="text-red-500">*</span>
            </label>
            <Input
              type="text"
              placeholder="0912888999 hoặc lan.hoang@email.com"
              value={guardianContact}
              onChange={(e) => setGuardianContact(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Ngày ghi nhận đồng ý <span className="text-red-500">*</span>
            </label>
            <Input
              type="date"
              max={todayStr}
              value={consentedOn}
              onChange={(e) => setConsentedOn(e.target.value)}
              required
            />
          </div>

          <div className="pt-4 border-t flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Đóng
            </Button>
            <Button type="submit" disabled={consentMutation.isPending}>
              <CheckCircle className="w-4 h-4 mr-1.5" />
              {consentMutation.isPending ? 'Đang lưu…' : 'Xác nhận đồng ý'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
