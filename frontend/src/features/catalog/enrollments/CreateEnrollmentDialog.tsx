import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { createEnrollment, fetchPackages, type PackageItem } from '@/lib/api/catalog'
import { fetchUsers, type UserItem } from '@/lib/api/users'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { X, BookOpen, UserCheck, AlertCircle } from 'lucide-react'

interface CreateEnrollmentDialogProps {
  defaultStudentId?: string
  open: boolean
  onClose: () => void
}

export default function CreateEnrollmentDialog({
  defaultStudentId,
  open,
  onClose,
}: CreateEnrollmentDialogProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [studentId, setStudentId] = useState(defaultStudentId || '')
  const [packageId, setPackageId] = useState('')
  const [teacherId, setTeacherId] = useState('')
  const [priceVnd, setPriceVnd] = useState<number | ''>('')

  // Lấy danh sách học viên
  const { data: students = [] } = useQuery({
    queryKey: ['users', 'STUDENT'],
    queryFn: () => fetchUsers({ role: 'STUDENT', status: 'ACTIVE' }),
    enabled: open && !defaultStudentId,
  })

  // Lấy danh sách giáo viên
  const { data: teachers = [] } = useQuery({
    queryKey: ['users', 'TEACHER'],
    queryFn: () => fetchUsers({ role: 'TEACHER', status: 'ACTIVE' }),
    enabled: open,
  })

  // Lấy danh sách gói học đang mở bán
  const { data: packages = [] } = useQuery({
    queryKey: ['packages'],
    queryFn: () => fetchPackages(),
    enabled: open,
  })

  const activePackages = packages.filter((p: PackageItem) => p.status === 'ACTIVE')

  // Khi chọn gói học, tự động điền học phí mặc định từ gói (T055)
  const handleSelectPackage = (pkgId: string) => {
    setPackageId(pkgId)
    const pkg = activePackages.find((p: PackageItem) => p.id === pkgId)
    if (pkg) {
      setPriceVnd(pkg.priceVnd)
    }
  }

  const createMutation = useMutation({
    mutationFn: () =>
      createEnrollment({
        studentId: defaultStudentId || studentId,
        packageId,
        teacherId,
        priceVnd: priceVnd !== '' ? Number(priceVnd) : undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrollments'] })
      queryClient.invalidateQueries({ queryKey: ['students'] })
      toast.success('Đã tạo ghi danh mới thành công! Trạng thái: Chờ thanh toán (PENDING_PAYMENT).')
      onClose()
      setPackageId('')
      setTeacherId('')
      setPriceVnd('')
    },
    onError: (err) => showError(err),
  })

  if (!open) return null

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const targetStudentId = defaultStudentId || studentId
    if (!targetStudentId || !packageId || !teacherId) {
      toast.error('Vui lòng chọn đầy đủ Học viên, Gói học và Giáo viên phụ trách!')
      return
    }
    createMutation.mutate()
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="p-5 border-b flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-indigo-600" />
            <h2 className="text-lg font-bold text-slate-800">Tạo ghi danh học tập mới</h2>
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
          {!defaultStudentId && (
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Học viên <span className="text-red-500">*</span>
              </label>
              <select
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
                className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
                required
              >
                <option value="">-- Chọn học viên --</option>
                {students.map((s: UserItem) => (
                  <option key={s.id} value={s.id}>
                    {s.fullName} ({s.email})
                  </option>
                ))}
              </select>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Gói học đăng ký <span className="text-red-500">*</span>
            </label>
            <select
              value={packageId}
              onChange={(e) => handleSelectPackage(e.target.value)}
              className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
              required
            >
              <option value="">-- Chọn gói học --</option>
              {activePackages.map((p: PackageItem) => (
                <option key={p.id} value={p.id}>
                  {p.name} — {p.sessionCount} buổi ({p.sessionMinutes}p) —{' '}
                  {p.priceVnd.toLocaleString('vi-VN')} đ
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Giáo viên 1 kèm 1 phụ trách <span className="text-red-500">*</span>
            </label>
            <select
              value={teacherId}
              onChange={(e) => setTeacherId(e.target.value)}
              className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
              required
            >
              <option value="">-- Phân công giáo viên --</option>
              {teachers.map((t: UserItem) => (
                <option key={t.id} value={t.id}>
                  {t.fullName} ({t.email})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Học phí áp dụng (VND){' '}
              <span className="text-slate-400 font-normal">(cho phép chỉnh sửa chỉ lúc tạo)</span>
            </label>
            <Input
              type="number"
              min={0}
              step={50000}
              value={priceVnd}
              onChange={(e) => setPriceVnd(e.target.value ? Number(e.target.value) : '')}
              placeholder="Học phí thực thu"
              required
            />
          </div>

          <div className="p-3 bg-slate-50 border rounded-xl text-xs text-slate-600 space-y-1">
            <div className="font-semibold text-slate-800 flex items-center gap-1.5">
              <AlertCircle className="w-4 h-4 text-indigo-600" />
              Quy trình kích hoạt:
            </div>
            <p>
              Ghi danh ban đầu sẽ ở trạng thái <strong>Chờ thanh toán (PENDING_PAYMENT)</strong>.
              Sau khi học viên thanh toán chuyển khoản hoặc tiền mặt, gói học sẽ chính thức được
              kích hoạt và học viên có thể đặt lịch học.
            </p>
          </div>

          <div className="pt-4 border-t flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={createMutation.isPending}>
              <UserCheck className="w-4 h-4 mr-1.5" />
              {createMutation.isPending ? 'Đang tạo…' : 'Xác nhận ghi danh'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
