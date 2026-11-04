import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '@/app/AuthProvider'
import {
  fetchTeacherAvailability,
  saveTeacherAvailability,
  fetchTeacherExceptions,
  addTeacherException,
  deleteTeacherException,
  type DayOfWeek,
  type AvailabilityRule,
} from '@/lib/api/scheduling'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { Plus, Trash2, Calendar, Clock, AlertCircle } from 'lucide-react'

const DAYS: { key: DayOfWeek; label: string }[] = [
  { key: 'MONDAY', label: 'Thứ Hai' },
  { key: 'TUESDAY', label: 'Thứ Ba' },
  { key: 'WEDNESDAY', label: 'Thứ Tư' },
  { key: 'THURSDAY', label: 'Thứ Năm' },
  { key: 'FRIDAY', label: 'Thứ Sáu' },
  { key: 'SATURDAY', label: 'Thứ Bảy' },
  { key: 'SUNDAY', label: 'Chủ Nhật' },
]

export default function AvailabilityEditor({ teacherIdOverride }: { teacherIdOverride?: string }) {
  const { me } = useAuth()
  const teacherId = teacherIdOverride || me?.id || ''
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [rules, setRules] = useState<AvailabilityRule[]>([])
  const [selectedDay, setSelectedDay] = useState<DayOfWeek>('MONDAY')
  const [newStart, setNewStart] = useState('08:00')
  const [newEnd, setNewEnd] = useState('12:00')

  // New exception form
  const [exDate, setExDate] = useState('')
  const [exStart, setExStart] = useState('')
  const [exEnd, setExEnd] = useState('')
  const [exReason, setExReason] = useState('')

  const { data: serverRules, isLoading: loadingRules } = useQuery({
    queryKey: ['teacher-availability', teacherId],
    queryFn: () => fetchTeacherAvailability(teacherId),
    enabled: !!teacherId,
  })

  const { data: exceptions = [], isLoading: loadingExceptions } = useQuery({
    queryKey: ['teacher-exceptions', teacherId],
    queryFn: () => fetchTeacherExceptions(teacherId),
    enabled: !!teacherId,
  })

  useEffect(() => {
    if (serverRules) {
      setRules(serverRules)
    }
  }, [serverRules])

  const saveMutation = useMutation({
    mutationFn: (updated: AvailabilityRule[]) => saveTeacherAvailability(teacherId, updated),
    onSuccess: (data) => {
      setRules(data)
      queryClient.invalidateQueries({ queryKey: ['teacher-availability', teacherId] })
      toast.success('Đã lưu lịch rảnh định kỳ thành công!')
    },
    onError: (err) => showError(err),
  })

  const addExceptionMutation = useMutation({
    mutationFn: () =>
      addTeacherException(teacherId, {
        date: exDate,
        startTime: exStart || null,
        endTime: exEnd || null,
        reason: exReason || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teacher-exceptions', teacherId] })
      toast.success('Đã thêm ngày nghỉ ngoại lệ!')
      setExDate('')
      setExStart('')
      setExEnd('')
      setExReason('')
    },
    onError: (err) => showError(err),
  })

  const deleteExceptionMutation = useMutation({
    mutationFn: (id: string) => deleteTeacherException(teacherId, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teacher-exceptions', teacherId] })
      toast.success('Đã xóa ngày nghỉ!')
    },
    onError: (err) => showError(err),
  })

  const handleAddInterval = () => {
    if (newStart >= newEnd) {
      toast.error('Giờ bắt đầu phải trước giờ kết thúc')
      return
    }
    const isOverlapping = rules
      .filter((r) => r.dayOfWeek === selectedDay)
      .some((r) => newStart < r.endTime && newEnd > r.startTime)
    if (isOverlapping) {
      toast.error('Khung giờ này bị trùng hoặc chồng lấn với khung giờ đã có trong ngày!')
      return
    }
    const updated = [...rules, { dayOfWeek: selectedDay, startTime: newStart, endTime: newEnd }]
    setRules(updated)
  }

  const handleSaveRules = () => {
    for (const d of DAYS) {
      const dayRules = rules
        .filter((r) => r.dayOfWeek === d.key)
        .sort((a, b) => a.startTime.localeCompare(b.startTime))
      for (let i = 1; i < dayRules.length; i++) {
        if (dayRules[i - 1].endTime > dayRules[i].startTime) {
          toast.error(
            `Ngày ${d.label} có khung giờ bị trùng hoặc chồng lấn (${dayRules[i - 1].startTime}-${dayRules[i - 1].endTime} và ${dayRules[i].startTime}-${dayRules[i].endTime})! Vui lòng xóa bớt trước khi lưu.`,
          )
          setSelectedDay(d.key)
          return
        }
      }
    }
    saveMutation.mutate(rules)
  }

  const handleRemoveRule = (index: number) => {
    setRules(rules.filter((_, i) => i !== index))
  }

  const currentDayRules = rules
    .map((r, originalIndex) => ({ ...r, originalIndex }))
    .filter((r) => r.dayOfWeek === selectedDay)

  return (
    <div className="space-y-8 max-w-4xl mx-auto p-4 sm:p-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
          Cài đặt Lịch rảnh & Ngày nghỉ
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Học viên sẽ chỉ có thể đặt lịch học vào các khung giờ bạn đã cấu hình bên dưới. Múi giờ áp
          dụng: <strong>{me?.timezone || 'Asia/Ho_Chi_Minh'}</strong>.
        </p>
      </div>

      {/* 1. Lịch rảnh tuần */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-4 border-b">
          <CardTitle className="text-lg flex items-center gap-2">
            <Clock className="w-5 h-5 text-indigo-600" />
            Khung giờ rảnh lặp lại hàng tuần
          </CardTitle>
          <Button
            onClick={handleSaveRules}
            disabled={saveMutation.isPending || loadingRules}
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-medium"
          >
            {saveMutation.isPending ? 'Đang lưu…' : 'Lưu thay đổi'}
          </Button>
        </CardHeader>
        <CardContent className="pt-6 space-y-6">
          {/* Day Tabs */}
          <div className="flex flex-wrap gap-2 border-b pb-4">
            {DAYS.map((d) => {
              const count = rules.filter((r) => r.dayOfWeek === d.key).length
              return (
                <button
                  key={d.key}
                  type="button"
                  onClick={() => setSelectedDay(d.key)}
                  className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 ${
                    selectedDay === d.key
                      ? 'bg-indigo-600 text-white shadow-sm'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {d.label}
                  {count > 0 && (
                    <span
                      className={`text-xs px-1.5 py-0.5 rounded-full ${
                        selectedDay === d.key
                          ? 'bg-indigo-800 text-white'
                          : 'bg-slate-300 text-slate-800'
                      }`}
                    >
                      {count}
                    </span>
                  )}
                </button>
              )
            })}
          </div>

          {/* List intervals for selected day */}
          <div className="space-y-3">
            <h3 className="text-sm font-semibold text-slate-700">
              Khung giờ rảnh trong ngày{' '}
              <span className="text-indigo-600 font-bold">
                {DAYS.find((d) => d.key === selectedDay)?.label}
              </span>
              :
            </h3>

            {currentDayRules.length === 0 ? (
              <div className="p-4 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-sm flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-amber-600" />
                Chưa có khung giờ rảnh nào được thiết lập cho ngày này. Bạn sẽ nghỉ cả ngày.
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {currentDayRules.map((rule) => (
                  <div
                    key={rule.originalIndex}
                    className="flex items-center justify-between p-3 rounded-lg border bg-slate-50 hover:bg-white transition-colors"
                  >
                    <span className="font-mono text-sm font-medium text-slate-800">
                      {rule.startTime.slice(0, 5)} - {rule.endTime.slice(0, 5)}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleRemoveRule(rule.originalIndex)}
                      className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded"
                      title="Xóa khung giờ"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Add interval inputs */}
          <div className="p-4 rounded-xl border border-dashed border-slate-300 bg-slate-50/50 flex flex-wrap items-center gap-3">
            <span className="text-sm font-medium text-slate-600">Thêm khung giờ mới:</span>
            <div className="flex items-center gap-2">
              <Input
                type="time"
                value={newStart}
                onChange={(e) => setNewStart(e.target.value)}
                className="w-28 bg-white"
              />
              <span className="text-slate-400">đến</span>
              <Input
                type="time"
                value={newEnd}
                onChange={(e) => setNewEnd(e.target.value)}
                className="w-28 bg-white"
              />
            </div>
            <Button
              type="button"
              variant="outline"
              onClick={handleAddInterval}
              className="flex items-center gap-1.5 text-sm"
            >
              <Plus className="w-4 h-4" /> Thêm khung giờ
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 2. Ngày nghỉ ngoại lệ */}
      <Card>
        <CardHeader className="border-b pb-4">
          <CardTitle className="text-lg flex items-center gap-2">
            <Calendar className="w-5 h-5 text-rose-600" />
            Ngày nghỉ ngoại lệ (Nghỉ phép / Nghỉ lễ)
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-6 space-y-6">
          <p className="text-sm text-slate-500">
            Nếu bạn có việc đột xuất hoặc nghỉ lễ trong một ngày cụ thể, hãy thêm vào đây. Hệ thống
            sẽ tự động khóa toàn bộ slot trong khoảng thời gian đó.
          </p>

          {/* Form thêm ngoại lệ */}
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 items-end p-4 rounded-lg bg-slate-50 border">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Ngày nghỉ *</label>
              <Input
                type="date"
                value={exDate}
                onChange={(e) => setExDate(e.target.value)}
                className="bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">
                Từ giờ (tùy chọn)
              </label>
              <Input
                type="time"
                value={exStart}
                onChange={(e) => setExStart(e.target.value)}
                className="bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">
                Đến giờ (tùy chọn)
              </label>
              <Input
                type="time"
                value={exEnd}
                onChange={(e) => setExEnd(e.target.value)}
                className="bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Lý do nghỉ</label>
              <Input
                type="text"
                placeholder="Khám bệnh, việc bận..."
                value={exReason}
                onChange={(e) => setExReason(e.target.value)}
                className="bg-white"
              />
            </div>
            <div className="sm:col-span-4 flex justify-end">
              <Button
                type="button"
                onClick={() => addExceptionMutation.mutate()}
                disabled={!exDate || addExceptionMutation.isPending}
                className="bg-rose-600 hover:bg-rose-700 text-white text-sm"
              >
                {addExceptionMutation.isPending ? 'Đang thêm…' : 'Xác nhận thêm ngày nghỉ'}
              </Button>
            </div>
          </div>

          {/* Danh sách ngoại lệ đã thêm */}
          <div className="space-y-2">
            <h4 className="text-sm font-semibold text-slate-700">Các ngày nghỉ đã đăng ký:</h4>
            {loadingExceptions ? (
              <p className="text-sm text-slate-400">Đang tải ngày nghỉ…</p>
            ) : exceptions.length === 0 ? (
              <p className="text-sm text-slate-400 italic">Chưa có ngày nghỉ ngoại lệ nào.</p>
            ) : (
              <div className="divide-y border rounded-lg overflow-hidden bg-white">
                {exceptions.map((ex) => (
                  <div
                    key={ex.id}
                    className="p-3 flex items-center justify-between hover:bg-slate-50 transition-colors"
                  >
                    <div>
                      <div className="font-medium text-slate-800 text-sm">
                        {ex.date}
                        {ex.startTime && ex.endTime && (
                          <span className="text-slate-500 font-normal ml-2">
                            ({ex.startTime.slice(0, 5)} - {ex.endTime.slice(0, 5)})
                          </span>
                        )}
                        {!ex.startTime && (
                          <span className="text-rose-600 font-normal ml-2 text-xs bg-rose-50 px-2 py-0.5 rounded-full">
                            Nghỉ cả ngày
                          </span>
                        )}
                      </div>
                      {ex.reason && <p className="text-xs text-slate-500 mt-0.5">{ex.reason}</p>}
                    </div>
                    {ex.id && (
                      <button
                        type="button"
                        onClick={() => deleteExceptionMutation.mutate(ex.id!)}
                        className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded"
                        title="Xóa ngày nghỉ này"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
