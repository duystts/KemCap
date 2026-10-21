import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  fetchPackages,
  fetchCourses,
  updatePackage,
  type PackageItem,
  type CourseItem,
} from '@/lib/api/catalog'
import PackageFormDialog from './PackageFormDialog'
import { Button } from '@/components/ui/button'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { Package, Plus, Edit, Clock, Calendar, Power } from 'lucide-react'

export default function PackagesPage() {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [selectedCourseId, setSelectedCourseId] = useState('')
  const [formDialogOpen, setFormDialogOpen] = useState(false)
  const [editingPackage, setEditingPackage] = useState<PackageItem | null>(null)

  const { data: courses = [] } = useQuery({
    queryKey: ['courses'],
    queryFn: fetchCourses,
  })

  const { data: packages = [], isLoading } = useQuery({
    queryKey: ['packages', selectedCourseId],
    queryFn: () => fetchPackages(selectedCourseId || undefined),
  })

  const statusMutation = useMutation({
    mutationFn: ({ pkg, status }: { pkg: PackageItem; status: 'ACTIVE' | 'ARCHIVED' }) =>
      updatePackage(pkg.id, {
        name: pkg.name,
        sessionMinutes: pkg.sessionMinutes,
        sessionCount: pkg.sessionCount,
        priceVnd: pkg.priceVnd,
        validityDays: pkg.validityDays,
        status,
      }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['packages'] })
      toast.success(
        variables.status === 'ACTIVE'
          ? 'Đã mở bán lại gói học!'
          : 'Đã lưu trữ / ngừng bán gói học!',
      )
    },
    onError: (err) => showError(err),
  })

  const getCourseName = (courseId: string) => {
    return courses.find((c: CourseItem) => c.id === courseId)?.name || 'Khóa học'
  }

  const formatVnd = (amount: number) => {
    return amount.toLocaleString('vi-VN') + ' đ'
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Package className="w-6 h-6 text-indigo-600" />
            Quản lý gói học
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Thiết lập số buổi, thời lượng (25/45/60/90 phút), học phí và thời hạn sử dụng gói.
          </p>
        </div>
        <Button
          onClick={() => {
            setEditingPackage(null)
            setFormDialogOpen(true)
          }}
          className="shrink-0"
        >
          <Plus className="w-4 h-4 mr-2" />
          Tạo gói học mới
        </Button>
      </div>

      {/* Filter by course */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 flex items-center gap-3">
        <label className="text-xs font-semibold text-slate-600">Lọc theo khóa học:</label>
        <select
          value={selectedCourseId}
          onChange={(e) => setSelectedCourseId(e.target.value)}
          className="px-3 py-1.5 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
        >
          <option value="">Tất cả khóa học</option>
          {courses.map((c: CourseItem) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      {/* Grid of packages */}
      {isLoading ? (
        <div className="p-12 text-center text-sm text-slate-400">Đang tải gói học…</div>
      ) : packages.length === 0 ? (
        <div className="p-12 text-center text-sm text-slate-500 bg-white rounded-xl border border-dashed">
          Chưa có gói học nào. Bấm &quot;Tạo gói học mới&quot; để thêm.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {packages.map((pkg: PackageItem) => {
            const isArchived = pkg.status === 'ARCHIVED'
            return (
              <div
                key={pkg.id}
                className={`bg-white rounded-2xl border transition-all p-5 flex flex-col justify-between ${
                  isArchived
                    ? 'border-slate-200 opacity-60 bg-slate-50'
                    : 'border-slate-200 hover:border-indigo-200 hover:shadow-md'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between text-xs text-slate-500 mb-2">
                    <span className="font-semibold text-indigo-600 uppercase tracking-wider">
                      {getCourseName(pkg.courseId)}
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded-full font-medium text-[11px] ${
                        isArchived
                          ? 'bg-slate-200 text-slate-700'
                          : 'bg-emerald-100 text-emerald-800'
                      }`}
                    >
                      {isArchived ? 'Ngừng bán' : 'Đang mở bán'}
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-slate-900">{pkg.name}</h3>
                  <div className="text-2xl font-black text-indigo-600 mt-2">
                    {formatVnd(pkg.priceVnd)}
                  </div>

                  <div className="mt-4 space-y-2 text-xs text-slate-600 pt-3 border-t">
                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1.5 text-slate-500">
                        <Clock className="w-3.5 h-3.5" /> Thời lượng:
                      </span>
                      <strong className="text-slate-800">{pkg.sessionMinutes} phút / buổi</strong>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1.5 text-slate-500">
                        <Package className="w-3.5 h-3.5" /> Tổng số buổi:
                      </span>
                      <strong className="text-slate-800">{pkg.sessionCount} buổi</strong>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="flex items-center gap-1.5 text-slate-500">
                        <Calendar className="w-3.5 h-3.5" /> Hạn sử dụng:
                      </span>
                      <strong className="text-slate-800">{pkg.validityDays} ngày</strong>
                    </div>
                  </div>
                </div>

                <div className="pt-4 mt-4 border-t flex items-center justify-end gap-2">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => {
                      setEditingPackage(pkg)
                      setFormDialogOpen(true)
                    }}
                    className="h-8 text-xs text-slate-600 hover:text-indigo-600"
                  >
                    <Edit className="w-3.5 h-3.5 mr-1" />
                    Sửa
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() =>
                      statusMutation.mutate({
                        pkg,
                        status: isArchived ? 'ACTIVE' : 'ARCHIVED',
                      })
                    }
                    disabled={statusMutation.isPending}
                    className={`h-8 text-xs ${
                      isArchived
                        ? 'text-emerald-600 hover:bg-emerald-50'
                        : 'text-slate-500 hover:text-red-600 hover:bg-red-50'
                    }`}
                  >
                    <Power className="w-3.5 h-3.5 mr-1" />
                    {isArchived ? 'Mở bán lại' : 'Ngừng bán'}
                  </Button>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Package Form Dialog */}
      <PackageFormDialog
        packageToEdit={editingPackage}
        defaultCourseId={selectedCourseId}
        open={formDialogOpen}
        onClose={() => {
          setFormDialogOpen(false)
          setEditingPackage(null)
        }}
      />
    </div>
  )
}
