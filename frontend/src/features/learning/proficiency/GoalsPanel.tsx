import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  fetchGoals,
  createGoal,
  updateGoal,
  type GoalItem,
  type GoalStatus,
} from '@/lib/api/learning'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { Target, Plus, CheckCircle, AlertCircle, X, Calendar } from 'lucide-react'

interface GoalsPanelProps {
  studentId: string
  canEdit?: boolean
}

export default function GoalsPanel({ studentId, canEdit = true }: GoalsPanelProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [goalType, setGoalType] = useState('IELTS Academic')
  const [target, setTarget] = useState('')
  const [deadline, setDeadline] = useState('')

  const { data: goals = [], isLoading } = useQuery({
    queryKey: ['goals', studentId],
    queryFn: () => fetchGoals(studentId),
  })

  const activeGoals = goals.filter((g: GoalItem) => g.status === 'ACTIVE')
  const hasActiveGoal = activeGoals.length > 0

  const createMutation = useMutation({
    mutationFn: () =>
      createGoal(studentId, {
        type: goalType.trim(),
        target: target.trim(),
        deadline: deadline || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals', studentId] })
      queryClient.invalidateQueries({ queryKey: ['proficiency', studentId] })
      queryClient.invalidateQueries({ queryKey: ['students'] })
      toast.success('Đã thiết lập mục tiêu học tập mới thành công!')
      setDialogOpen(false)
      setTarget('')
      setDeadline('')
    },
    onError: (err) => showError(err),
  })

  const updateMutation = useMutation({
    mutationFn: ({ goalId, status }: { goalId: string; status: GoalStatus }) =>
      updateGoal(studentId, goalId, { status }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['goals', studentId] })
      queryClient.invalidateQueries({ queryKey: ['proficiency', studentId] })
      queryClient.invalidateQueries({ queryKey: ['students'] })
      toast.success(
        variables.status === 'ACHIEVED'
          ? 'Chúc mừng học viên đã đạt được mục tiêu!'
          : 'Đã đóng mục tiêu học tập.',
      )
    },
    onError: (err) => showError(err),
  })

  if (isLoading) {
    return <div className="p-8 text-center text-sm text-slate-400">Đang tải mục tiêu…</div>
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-3">
        <div className="flex items-center gap-2">
          <Target className="w-5 h-5 text-indigo-600" />
          <CardTitle className="text-base font-bold text-slate-800">
            Mục tiêu học tập cá nhân
          </CardTitle>
          {!hasActiveGoal && (
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200 ml-2">
              <AlertCircle className="w-3.5 h-3.5 text-amber-600" />
              Cần tư vấn
            </span>
          )}
        </div>
        {canEdit && (
          <Button size="sm" onClick={() => setDialogOpen(true)}>
            <Plus className="w-4 h-4 mr-1.5" />
            Thiết lập mục tiêu
          </Button>
        )}
      </CardHeader>

      <CardContent>
        {goals.length === 0 ? (
          <div className="p-6 text-center bg-amber-50/50 border border-amber-200 rounded-xl">
            <AlertCircle className="w-6 h-6 text-amber-600 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-800">
              Học viên chưa thiết lập mục tiêu học tập
            </p>
            <p className="text-xs text-slate-600 mt-1 max-w-md mx-auto">
              Hệ thống đang đánh dấu trạng thái <strong>&quot;Cần tư vấn&quot;</strong>. Giáo viên
              hoặc giáo vụ hãy liên hệ để xác định mục tiêu và thời hạn cam kết.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {goals.map((g: GoalItem) => {
              const isActive = g.status === 'ACTIVE'
              const isAchieved = g.status === 'ACHIEVED'
              return (
                <div
                  key={g.id}
                  className={`p-4 rounded-xl border flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                    isActive
                      ? 'bg-white border-indigo-200 shadow-2xs'
                      : isAchieved
                        ? 'bg-emerald-50/50 border-emerald-200'
                        : 'bg-slate-50 border-slate-200 opacity-60'
                  }`}
                >
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-bold text-indigo-600 uppercase tracking-wider">
                        {g.type}
                      </span>
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          isActive
                            ? 'bg-indigo-100 text-indigo-800'
                            : isAchieved
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-slate-200 text-slate-700'
                        }`}
                      >
                        {isActive ? 'Đang thực hiện' : isAchieved ? 'Đã đạt' : 'Đã dừng'}
                      </span>
                    </div>

                    <div className="text-sm font-bold text-slate-900">{g.target}</div>

                    <div className="flex items-center gap-3 text-xs text-slate-500 mt-1">
                      {g.deadline && (
                        <span className="flex items-center gap-1 text-slate-600 font-medium">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          Hạn chót: {new Date(g.deadline).toLocaleDateString('vi-VN')}
                        </span>
                      )}
                      <span>Tạo ngày: {new Date(g.createdAt).toLocaleDateString('vi-VN')}</span>
                    </div>
                  </div>

                  {canEdit && isActive && (
                    <div className="flex items-center gap-2 shrink-0">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => updateMutation.mutate({ goalId: g.id, status: 'ACHIEVED' })}
                        disabled={updateMutation.isPending}
                        className="h-8 text-xs text-emerald-700 border-emerald-300 hover:bg-emerald-50"
                      >
                        <CheckCircle className="w-3.5 h-3.5 mr-1 text-emerald-600" />
                        Đạt mục tiêu
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => updateMutation.mutate({ goalId: g.id, status: 'DROPPED' })}
                        disabled={updateMutation.isPending}
                        className="h-8 text-xs text-slate-500 hover:text-red-600 hover:bg-red-50"
                      >
                        Đóng
                      </Button>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </CardContent>

      {/* Dialog tạo mục tiêu */}
      {dialogOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="p-5 border-b flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Target className="w-5 h-5 text-indigo-600" />
                <h2 className="text-lg font-bold text-slate-800">Thiết lập mục tiêu học tập</h2>
              </div>
              <button
                type="button"
                onClick={() => setDialogOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                if (!target.trim()) {
                  toast.error('Vui lòng nhập mục tiêu mong muốn!')
                  return
                }
                createMutation.mutate()
              }}
              className="p-6 space-y-4"
            >
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Loại mục tiêu <span className="text-red-500">*</span>
                </label>
                <select
                  value={goalType}
                  onChange={(e) => setGoalType(e.target.value)}
                  className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
                >
                  <option value="IELTS Academic">IELTS Academic</option>
                  <option value="IELTS General">IELTS General</option>
                  <option value="Tiếng Anh giao tiếp">Tiếng Anh giao tiếp</option>
                  <option value="Tiếng Anh công sở / Phỏng vấn">
                    Tiếng Anh công sở / Phỏng vấn
                  </option>
                  <option value="TOEIC">TOEIC</option>
                  <option value="Khác">Khác</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Mục tiêu cụ thể (Target) <span className="text-red-500">*</span>
                </label>
                <Input
                  placeholder="Ví dụ: Đạt 6.5 Overall (Speaking ≥ 6.0)"
                  value={target}
                  onChange={(e) => setTarget(e.target.value)}
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Thời hạn dự kiến hoàn thành
                </label>
                <Input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} />
              </div>

              <div className="pt-4 border-t flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                  Hủy
                </Button>
                <Button type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending ? 'Đang lưu…' : 'Lưu mục tiêu'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Card>
  )
}
