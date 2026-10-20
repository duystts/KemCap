import { useState, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  createPackage,
  updatePackage,
  fetchCourses,
  type PackageItem,
  type CourseItem,
} from '@/lib/api/catalog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { X, Package as PackageIcon } from 'lucide-react'

interface PackageFormDialogProps {
  packageToEdit?: PackageItem | null
  defaultCourseId?: string
  open: boolean
  onClose: () => void
}

export default function PackageFormDialog({
  packageToEdit,
  defaultCourseId,
  open,
  onClose,
}: PackageFormDialogProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const { data: courses = [] } = useQuery({
    queryKey: ['courses'],
    queryFn: fetchCourses,
    enabled: open,
  })

  const [courseId, setCourseId] = useState(defaultCourseId || '')
  const [name, setName] = useState('')
  const [sessionMinutes, setSessionMinutes] = useState<25 | 45 | 60 | 90>(60)
  const [sessionCount, setSessionCount] = useState(10)
  const [priceVnd, setPriceVnd] = useState(4500000)
  const [validityDays, setValidityDays] = useState(60)

  useEffect(() => {
    if (packageToEdit) {
      setCourseId(packageToEdit.courseId)
      setName(packageToEdit.name)
      setSessionMinutes(packageToEdit.sessionMinutes)
      setSessionCount(packageToEdit.sessionCount)
      setPriceVnd(packageToEdit.priceVnd)
      setValidityDays(packageToEdit.validityDays)
    } else {
      setCourseId(defaultCourseId || courses[0]?.id || '')
      setName('')
      setSessionMinutes(60)
      setSessionCount(10)
      setPriceVnd(4500000)
      setValidityDays(60)
    }
  }, [packageToEdit, defaultCourseId, courses, open])

  const saveMutation = useMutation({
    mutationFn: () => {
      if (packageToEdit) {
        return updatePackage(packageToEdit.id, {
          name: name.trim(),
          sessionMinutes,
          sessionCount: Number(sessionCount),
          priceVnd: Number(priceVnd),
          validityDays: Number(validityDays),
        })
      } else {
        return createPackage({
          courseId,
          name: name.trim(),
          sessionMinutes,
          sessionCount: Number(sessionCount),
          priceVnd: Number(priceVnd),
          validityDays: Number(validityDays),
        })
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['packages'] })
      toast.success(
        packageToEdit ? 'Đã cập nhật gói học thành công!' : 'Đã tạo gói học mới thành công!',
      )
      onClose()
    },
    onError: (err) => showError(err),
  })

  if (!open) return null

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) {
      toast.error('Vui lòng nhập tên gói học!')
      return
    }
    if (!courseId) {
      toast.error('Vui lòng chọn khóa học tương ứng!')
      return
    }
    saveMutation.mutate()
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="p-5 border-b flex items-center justify-between">
          <div className="flex items-center gap-2">
            <PackageIcon className="w-5 h-5 text-indigo-600" />
            <h2 className="text-lg font-bold text-slate-800">
              {packageToEdit ? 'Chỉnh sửa gói học' : 'Tạo gói học mới'}
            </h2>
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
              Khóa học <span className="text-red-500">*</span>
            </label>
            <select
              value={courseId}
              onChange={(e) => setCourseId(e.target.value)}
              disabled={!!packageToEdit}
              className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white disabled:bg-slate-100"
              required
            >
              <option value="">-- Chọn khóa học --</option>
              {courses.map((c: CourseItem) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Tên gói học <span className="text-red-500">*</span>
            </label>
            <Input
              placeholder="Ví dụ: Gói 10 buổi (60 phút)"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Thời lượng mỗi buổi <span className="text-red-500">*</span>
              </label>
              <select
                value={sessionMinutes}
                onChange={(e) => setSessionMinutes(Number(e.target.value) as 25 | 45 | 60 | 90)}
                className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
              >
                <option value={25}>25 phút</option>
                <option value={45}>45 phút</option>
                <option value={60}>60 phút</option>
                <option value={90}>90 phút</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Số buổi học <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min={1}
                max={200}
                value={sessionCount}
                onChange={(e) => setSessionCount(Number(e.target.value))}
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Học phí (VND) <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min={0}
                step={50000}
                value={priceVnd}
                onChange={(e) => setPriceVnd(Number(e.target.value))}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Thời hạn (ngày) <span className="text-red-500">*</span>
              </label>
              <Input
                type="number"
                min={1}
                max={720}
                value={validityDays}
                onChange={(e) => setValidityDays(Number(e.target.value))}
                required
              />
            </div>
          </div>

          <div className="pt-4 border-t flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={saveMutation.isPending}>
              {saveMutation.isPending
                ? 'Đang lưu…'
                : packageToEdit
                  ? 'Lưu thay đổi'
                  : 'Tạo gói học'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
