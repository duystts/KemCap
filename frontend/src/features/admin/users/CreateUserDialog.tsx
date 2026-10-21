import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { createUser, type CreateUserData } from '@/lib/api/users'
import type { Role } from '@/lib/api/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { X, UserPlus, AlertCircle } from 'lucide-react'

interface CreateUserDialogProps {
  open: boolean
  onClose: () => void
}

export default function CreateUserDialog({ open, onClose }: CreateUserDialogProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [email, setEmail] = useState('')
  const [fullName, setFullName] = useState('')
  const [role, setRole] = useState<Role>('STUDENT')
  const [phone, setPhone] = useState('')
  const [dateOfBirth, setDateOfBirth] = useState('')

  // Tính tuổi
  let isUnder16 = false
  if (role === 'STUDENT' && dateOfBirth) {
    const bDate = new Date(dateOfBirth)
    const ageDiffMs = Date.now() - bDate.getTime()
    const age = new Date(ageDiffMs).getUTCFullYear() - 1970
    isUnder16 = age < 16
  }

  const createMutation = useMutation({
    mutationFn: (data: CreateUserData) => createUser(data),
    onSuccess: (newUser) => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      if (newUser.status === 'AWAITING_GUARDIAN_CONSENT') {
        toast.success('Đã tạo tài khoản học viên! Cần bổ sung thông tin đồng ý của người giám hộ.')
      } else {
        toast.success('Tạo tài khoản và gửi thư mời kích hoạt thành công!')
      }
      onClose()
      resetForm()
    },
    onError: (err) => showError(err),
  })

  const resetForm = () => {
    setEmail('')
    setFullName('')
    setRole('STUDENT')
    setPhone('')
    setDateOfBirth('')
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!email.trim() || !fullName.trim()) {
      toast.error('Vui lòng điền đầy đủ họ tên và email!')
      return
    }
    if (role === 'STUDENT' && !dateOfBirth) {
      toast.error('Ngày sinh là bắt buộc đối với học viên để xác định chính sách bảo vệ trẻ em!')
      return
    }

    createMutation.mutate({
      email: email.trim(),
      fullName: fullName.trim(),
      role,
      phone: phone.trim() || undefined,
      dateOfBirth: dateOfBirth || undefined,
    })
  }

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="p-5 border-b flex items-center justify-between">
          <div className="flex items-center gap-2">
            <UserPlus className="w-5 h-5 text-indigo-600" />
            <h2 className="text-lg font-bold text-slate-800">Thêm người dùng mới</h2>
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
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Vai trò <span className="text-red-500">*</span>
            </label>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
            >
              <option value="STUDENT">Học viên (Student)</option>
              <option value="TEACHER">Giáo viên (Teacher)</option>
              <option value="ADMIN">Giáo vụ / Quản trị viên (Admin)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Họ và tên <span className="text-red-500">*</span>
            </label>
            <Input
              type="text"
              placeholder="Ví dụ: Nguyễn Văn An"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Email đăng nhập <span className="text-red-500">*</span>
            </label>
            <Input
              type="email"
              placeholder="an.nguyen@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Số điện thoại
              </label>
              <Input
                type="tel"
                placeholder="0912345678"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Ngày sinh {role === 'STUDENT' && <span className="text-red-500">*</span>}
              </label>
              <Input
                type="date"
                value={dateOfBirth}
                onChange={(e) => setDateOfBirth(e.target.value)}
                required={role === 'STUDENT'}
              />
            </div>
          </div>

          {isUnder16 && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <strong>Học viên dưới 16 tuổi:</strong> Tài khoản sẽ ở trạng thái{' '}
                <em>Chờ đồng ý của người giám hộ</em>. Quản trị viên cần ghi nhận thông tin phụ
                huynh trước khi hệ thống gửi thư mời kích hoạt.
              </div>
            </div>
          )}

          <div className="pt-4 border-t flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Đang tạo…' : 'Tạo người dùng'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
