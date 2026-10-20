import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { fetchCourses, createCourse, updateCourse, type CourseItem } from '@/lib/api/catalog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { BookOpen, Plus, Edit, Package, X, Power } from 'lucide-react'

export default function CoursesPage() {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingCourse, setEditingCourse] = useState<CourseItem | null>(null)
  const [courseName, setCourseName] = useState('')
  const [courseDescription, setCourseDescription] = useState('')

  const { data: courses = [], isLoading } = useQuery({
    queryKey: ['courses'],
    queryFn: fetchCourses,
  })

  const saveMutation = useMutation({
    mutationFn: () => {
      if (editingCourse) {
        return updateCourse(editingCourse.id, {
          name: courseName.trim(),
          description: courseDescription.trim(),
        })
      } else {
        return createCourse({
          name: courseName.trim(),
          description: courseDescription.trim(),
        })
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['courses'] })
      toast.success(
        editingCourse ? 'Đã cập nhật khóa học thành công!' : 'Đã tạo khóa học mới thành công!',
      )
      closeDialog()
    },
    onError: (err) => showError(err),
  })

  const statusMutation = useMutation({
    mutationFn: ({ course, status }: { course: CourseItem; status: 'ACTIVE' | 'ARCHIVED' }) =>
      updateCourse(course.id, {
        name: course.name,
        description: course.description || undefined,
        status,
      }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['courses'] })
      toast.success(variables.status === 'ACTIVE' ? 'Đã mở lại khóa học!' : 'Đã lưu trữ khóa học!')
    },
    onError: (err) => showError(err),
  })

  const openCreateDialog = () => {
    setEditingCourse(null)
    setCourseName('')
    setCourseDescription('')
    setDialogOpen(true)
  }

  const openEditDialog = (c: CourseItem) => {
    setEditingCourse(c)
    setCourseName(c.name)
    setCourseDescription(c.description || '')
    setDialogOpen(true)
  }

  const closeDialog = () => {
    setDialogOpen(false)
    setEditingCourse(null)
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <BookOpen className="w-6 h-6 text-indigo-600" />
            Khóa học & Chương trình đào tạo
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý danh mục các chương trình học (IELTS, Giao tiếp, Tiếng Anh trẻ em,…) và liên kết
            với các gói học.
          </p>
        </div>
        <Button onClick={openCreateDialog} className="shrink-0">
          <Plus className="w-4 h-4 mr-2" />
          Tạo khóa học mới
        </Button>
      </div>

      {/* Course List */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-2xs">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-slate-400">Đang tải khóa học…</div>
        ) : courses.length === 0 ? (
          <div className="p-12 text-center text-sm text-slate-500">
            Chưa có khóa học nào. Bấm &quot;Tạo khóa học mới&quot; để bắt đầu.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b text-xs uppercase font-semibold text-slate-500">
                <tr>
                  <th className="py-3 px-4">Tên khóa học</th>
                  <th className="py-3 px-4">Mô tả</th>
                  <th className="py-3 px-4">Trạng thái</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {courses.map((c: CourseItem) => {
                  const isArchived = c.status === 'ARCHIVED'
                  return (
                    <tr key={c.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4 font-semibold text-slate-900">{c.name}</td>
                      <td className="py-3.5 px-4 text-xs text-slate-600 max-w-md">
                        {c.description || (
                          <span className="text-slate-400 italic">Chưa có mô tả</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                            isArchived
                              ? 'bg-slate-100 text-slate-700'
                              : 'bg-emerald-100 text-emerald-800'
                          }`}
                        >
                          {isArchived ? 'Đã lưu trữ' : 'Đang hoạt động'}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Link
                            to={`/admin/packages?courseId=${c.id}`}
                            className="inline-flex items-center text-xs px-2.5 py-1.5 rounded-lg border border-indigo-200 text-indigo-700 hover:bg-indigo-50 font-medium"
                          >
                            <Package className="w-3.5 h-3.5 mr-1" />
                            Gói học
                          </Link>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => openEditDialog(c)}
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
                                course: c,
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
                            {isArchived ? 'Mở lại' : 'Lưu trữ'}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Dialog tạo / sửa khóa học */}
      {dialogOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="p-5 border-b flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-800">
                {editingCourse ? 'Chỉnh sửa khóa học' : 'Tạo khóa học mới'}
              </h2>
              <button
                type="button"
                onClick={closeDialog}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                if (!courseName.trim()) {
                  toast.error('Vui lòng nhập tên khóa học!')
                  return
                }
                saveMutation.mutate()
              }}
              className="p-6 space-y-4"
            >
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Tên khóa học <span className="text-red-500">*</span>
                </label>
                <Input
                  placeholder="Ví dụ: IELTS Intensive 1 kèm 1"
                  value={courseName}
                  onChange={(e) => setCourseName(e.target.value)}
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Mô tả</label>
                <textarea
                  rows={3}
                  placeholder="Mô tả mục tiêu, đối tượng học viên của khóa học…"
                  value={courseDescription}
                  onChange={(e) => setCourseDescription(e.target.value)}
                  className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
                />
              </div>

              <div className="pt-4 border-t flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={closeDialog}>
                  Hủy
                </Button>
                <Button type="submit" disabled={saveMutation.isPending}>
                  {saveMutation.isPending
                    ? 'Đang lưu…'
                    : editingCourse
                      ? 'Lưu thay đổi'
                      : 'Tạo khóa học'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
