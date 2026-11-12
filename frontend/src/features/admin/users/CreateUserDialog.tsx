import { useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { createUser, type CreateUserData } from '@/lib/api/users'
import type { Role } from '@/lib/api/types'
import { Button } from '@/components/ui/button'
import { FieldError, Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { ApiError } from '@/lib/api/client'

// Khớp với @Pattern của CreateUserRequest ở backend
const PHONE_PATTERN = /^(0|\+84)\d{9,10}$/

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
  const [emailError, setEmailError] = useState('')
  const emailWrapRef = useRef<HTMLDivElement>(null)

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
    onError: (err) => {
      // Email trùng: báo ngay tại ô email và đưa con trỏ về đó thay vì chỉ hiện toast
      if (err instanceof ApiError && err.code === 'EMAIL_TAKEN') {
        setEmailError('Email này đã được sử dụng trong hệ thống.')
        emailWrapRef.current?.querySelector('input')?.focus()
        return
      }
      showError(err)
    },
  })

  const resetForm = () => {
    setEmail('')
    setFullName('')
    setRole('STUDENT')
    setPhone('')
    setDateOfBirth('')
    setEmailError('')
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

    // Cho phép gõ kèm dấu cách, chấm, gạch để dễ đọc; lưu dạng liền
    const normalizedPhone = phone.replace(/[\s.-]/g, '')
    if (normalizedPhone && !PHONE_PATTERN.test(normalizedPhone)) {
      toast.error('Số điện thoại không hợp lệ. Nhập 10–11 chữ số, bắt đầu bằng 0 hoặc +84.')
      return
    }

    createMutation.mutate({
      email: email.trim(),
      fullName: fullName.trim(),
      role,
      phone: normalizedPhone || undefined,
      dateOfBirth: dateOfBirth || undefined,
    })
  }

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="p-5 border-b flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-bold text-slate-800">Thêm người dùng mới</h2>
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

          <div ref={emailWrapRef}>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Email đăng nhập <span className="text-red-500">*</span>
            </label>
            <Input
              type="email"
              placeholder="an.nguyen@example.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value)
                setEmailError('')
              }}
              aria-invalid={!!emailError}
              required
            />
            <FieldError message={emailError} />
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
