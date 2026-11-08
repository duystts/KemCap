import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  updateLearningPath,
  createLearningPath,
  type LearningPathDto,
  type MilestoneItem,
  type TopicItem,
} from '@/lib/api/learning'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'

interface LearningPathEditorProps {
  studentId: string
  existingPath?: LearningPathDto | null
  open: boolean
  onClose: () => void
}

export default function LearningPathEditor({
  studentId,
  existingPath,
  open,
  onClose,
}: LearningPathEditorProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [title, setTitle] = useState(existingPath?.title || 'Lộ trình học tập cá nhân hóa')
  const [milestones, setMilestones] = useState<MilestoneItem[]>(() => {
    if (existingPath?.milestones && existingPath.milestones.length > 0) {
      return JSON.parse(JSON.stringify(existingPath.milestones))
    }
    return [
      {
        id: `ms-new-1`,
        position: 1,
        title: 'Chặng 1: Nền tảng & Củng cố',
        targetDate: '',
        topics: [
          {
            id: `top-new-1`,
            position: 1,
            title: 'Chủ đề 1',
            objective: '',
            status: 'PLANNED',
          },
        ],
      },
    ]
  })

  // Dialog nhập lý do điều chỉnh
  const [reasonDialogOpen, setReasonDialogOpen] = useState(false)
  const [revisionReason, setRevisionReason] = useState('')

  const saveMutation = useMutation({
    mutationFn: () => {
      if (existingPath) {
        return updateLearningPath(existingPath.id, {
          title: title.trim(),
          milestones: milestones.map((m) => ({
            id: m.id.startsWith('ms-new') ? undefined : m.id,
            title: m.title.trim(),
            targetDate: m.targetDate || undefined,
            topics: m.topics.map((t) => ({
              id: t.id.startsWith('top-new') ? undefined : t.id,
              title: t.title.trim(),
              objective: t.objective?.trim() || undefined,
            })),
          })),
          reason: revisionReason.trim(),
          version: existingPath.version ?? 0,
        })
      } else {
        return createLearningPath(studentId, {
          title: title.trim(),
          milestones: milestones.map((m) => ({
            title: m.title.trim(),
            targetDate: m.targetDate || undefined,
            topics: m.topics.map((t) => ({
              title: t.title.trim(),
              objective: t.objective?.trim() || undefined,
            })),
          })),
        })
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['learning-path', studentId] })
      toast.success(
        existingPath
          ? 'Đã cập nhật lộ trình học tập và lưu lịch sử điều chỉnh!'
          : 'Đã khởi tạo lộ trình học tập thành công!',
      )
      setReasonDialogOpen(false)
      onClose()
    },
    onError: (err) => showError(err),
  })

  if (!open) return null

  // Đổi chỗ phần tử i với i + delta (delta = -1 lên, 1 xuống)
  const swap = <T,>(items: T[], i: number, delta: number): T[] => {
    const j = i + delta
    if (j < 0 || j >= items.length) return items
    const next = [...items]
    ;[next[i], next[j]] = [next[j], next[i]]
    return next
  }

  const addMilestone = () => {
    const nextIdx = milestones.length + 1
    setMilestones([
      ...milestones,
      {
        id: `ms-new-${Date.now()}`,
        position: nextIdx,
        title: `Chặng ${nextIdx}`,
        targetDate: '',
        topics: [],
      },
    ])
  }

  const removeMilestone = (mIdx: number) => {
    setMilestones(milestones.filter((_, idx) => idx !== mIdx))
  }

  const moveMilestone = (mIdx: number, delta: number) => {
    setMilestones(swap(milestones, mIdx, delta))
  }

  const updateMilestone = (mIdx: number, field: 'title' | 'targetDate', value: string) => {
    setMilestones(milestones.map((m, idx) => (idx === mIdx ? { ...m, [field]: value } : m)))
  }

  const updateTopics = (mIdx: number, change: (topics: TopicItem[]) => TopicItem[]) => {
    setMilestones(
      milestones.map((m, idx) => (idx === mIdx ? { ...m, topics: change(m.topics) } : m)),
    )
  }

  const addTopic = (mIdx: number) =>
    updateTopics(mIdx, (topics) => [
      ...topics,
      {
        id: `top-new-${Date.now()}`,
        position: topics.length + 1,
        title: `Chủ đề ${topics.length + 1}`,
        objective: '',
        status: 'PLANNED',
      },
    ])

  const removeTopic = (mIdx: number, tIdx: number) =>
    updateTopics(mIdx, (topics) => topics.filter((_, idx) => idx !== tIdx))

  const moveTopic = (mIdx: number, tIdx: number, delta: number) =>
    updateTopics(mIdx, (topics) => swap(topics, tIdx, delta))

  const updateTopic = (mIdx: number, tIdx: number, field: 'title' | 'objective', value: string) =>
    updateTopics(mIdx, (topics) =>
      topics.map((t, idx) => (idx === tIdx ? { ...t, [field]: value } : t)),
    )

  const handlePreSave = () => {
    if (!title.trim()) {
      toast.error('Vui lòng nhập tiêu đề lộ trình!')
      return
    }
    if (milestones.length === 0) {
      toast.error('Lộ trình cần có ít nhất một chặng học!')
      return
    }
    if (milestones.some((m) => !m.title.trim() || m.topics.some((t) => !t.title.trim()))) {
      toast.error('Chặng và chủ đề đều cần có tên.')
      return
    }
    if (existingPath) {
      setRevisionReason('')
      setReasonDialogOpen(true)
    } else {
      saveMutation.mutate()
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto">
      <div className="bg-white rounded-2xl shadow-2xl max-w-4xl w-full max-h-[90vh] flex flex-col justify-between overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-5 border-b flex items-center justify-between shrink-0 bg-slate-50">
          <div>
            <h2 className="text-lg font-bold text-slate-800">
              {existingPath ? 'Điều chỉnh lộ trình học tập' : 'Thiết lập lộ trình học tập mới'}
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Cấu trúc các chặng mục tiêu, chủ đề bài học và chỉ tiêu năng lực.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-200"
          >
            Đóng
          </button>
        </div>

        {/* Form Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Tiêu đề lộ trình <span className="text-red-500">*</span>
            </label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Ví dụ: Lộ trình IELTS 6.5 Cá nhân hóa (40 buổi)"
              className="text-base font-semibold"
              required
            />
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-bold text-slate-800 uppercase tracking-wider">
                Danh sách các chặng học ({milestones.length})
              </span>
              <Button type="button" size="sm" variant="outline" onClick={addMilestone}>
                Thêm chặng mới
              </Button>
            </div>

            {milestones.map((m, mIdx) => (
              <div
                key={m.id}
                className="p-4 sm:p-5 rounded-2xl border border-slate-200 bg-slate-50/70 space-y-4 shadow-2xs"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex-1">
                    <label className="block text-[11px] font-semibold text-slate-500 mb-1">
                      Tên chặng {mIdx + 1}
                    </label>
                    <Input
                      aria-label={`Tên chặng ${mIdx + 1}`}
                      value={m.title}
                      onChange={(e) => updateMilestone(mIdx, 'title', e.target.value)}
                      placeholder="Tên chặng học…"
                      className="font-bold text-slate-900 bg-white"
                    />
                  </div>

                  <div className="w-full sm:w-48">
                    <label className="block text-[11px] font-semibold text-slate-500 mb-1">
                      Ngày mục tiêu hoàn thành
                    </label>
                    <Input
                      aria-label={`Ngày mục tiêu chặng ${mIdx + 1}`}
                      type="date"
                      value={m.targetDate || ''}
                      onChange={(e) => updateMilestone(mIdx, 'targetDate', e.target.value)}
                      className="bg-white text-xs"
                    />
                  </div>

                  <div className="flex items-center gap-1 self-end">
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() => moveMilestone(mIdx, -1)}
                      disabled={mIdx === 0}
                      aria-label={`Đưa chặng ${mIdx + 1} lên`}
                    >
                      Lên
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() => moveMilestone(mIdx, 1)}
                      disabled={mIdx === milestones.length - 1}
                      aria-label={`Đưa chặng ${mIdx + 1} xuống`}
                    >
                      Xuống
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() => removeMilestone(mIdx)}
                      className="text-red-600 border-red-200 hover:bg-red-50"
                      aria-label={`Xóa chặng ${mIdx + 1}`}
                    >
                      Xóa
                    </Button>
                  </div>
                </div>

                {/* Topics in Milestone */}
                <div className="pl-2 sm:pl-4 border-l-2 border-indigo-200 space-y-2.5">
                  <div className="flex items-center justify-between text-xs text-slate-500 font-semibold mb-1">
                    <span>Các chủ đề / bài học ({m.topics.length}):</span>
                    <button
                      type="button"
                      onClick={() => addTopic(mIdx)}
                      className="text-indigo-600 hover:text-indigo-800 text-xs flex items-center gap-1 font-bold"
                    >
                      Thêm chủ đề
                    </button>
                  </div>

                  {m.topics.length === 0 ? (
                    <div className="text-xs text-slate-400 italic py-2">
                      Chưa có chủ đề nào trong chặng này.
                    </div>
                  ) : (
                    m.topics.map((t, tIdx) => (
                      <div
                        key={t.id}
                        className="p-2.5 bg-white rounded-xl border border-slate-200 flex flex-col md:flex-row items-center gap-2"
                      >
                        <span className="text-xs font-bold text-slate-400 w-6 shrink-0">
                          #{tIdx + 1}
                        </span>

                        <Input
                          aria-label={`Tên chủ đề ${mIdx + 1}.${tIdx + 1}`}
                          value={t.title}
                          onChange={(e) => updateTopic(mIdx, tIdx, 'title', e.target.value)}
                          placeholder="Tiêu đề bài học / chủ đề…"
                          className="text-xs font-semibold flex-1"
                        />

                        <Input
                          aria-label={`Mục tiêu chủ đề ${mIdx + 1}.${tIdx + 1}`}
                          value={t.objective || ''}
                          onChange={(e) => updateTopic(mIdx, tIdx, 'objective', e.target.value)}
                          placeholder="Mục tiêu cụ thể…"
                          className="text-xs flex-1 text-slate-600"
                        />

                        <div className="flex items-center gap-1 shrink-0 text-xs">
                          <button
                            type="button"
                            onClick={() => moveTopic(mIdx, tIdx, -1)}
                            disabled={tIdx === 0}
                            aria-label={`Đưa chủ đề ${mIdx + 1}.${tIdx + 1} lên`}
                            className="px-2 py-1 rounded text-slate-600 hover:bg-slate-100 disabled:opacity-40"
                          >
                            Lên
                          </button>
                          <button
                            type="button"
                            onClick={() => moveTopic(mIdx, tIdx, 1)}
                            disabled={tIdx === m.topics.length - 1}
                            aria-label={`Đưa chủ đề ${mIdx + 1}.${tIdx + 1} xuống`}
                            className="px-2 py-1 rounded text-slate-600 hover:bg-slate-100 disabled:opacity-40"
                          >
                            Xuống
                          </button>
                          <button
                            type="button"
                            onClick={() => removeTopic(mIdx, tIdx)}
                            aria-label={`Xóa chủ đề ${mIdx + 1}.${tIdx + 1}`}
                            className="px-2 py-1 rounded text-red-600 hover:bg-red-50"
                          >
                            Xóa
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t bg-slate-50 flex items-center justify-between shrink-0">
          <Button type="button" variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button type="button" onClick={handlePreSave} disabled={saveMutation.isPending}>
            {saveMutation.isPending ? 'Đang lưu…' : 'Lưu lộ trình'}
          </Button>
        </div>
      </div>

      {/* Dialog nhập lý do sửa đổi lộ trình (FR-018) */}
      {reasonDialogOpen && (
        <div className="fixed inset-0 z-60 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center gap-2 text-amber-600 font-bold">
              <span>Ghi nhận lý do điều chỉnh lộ trình</span>
            </div>
            <p className="text-xs text-slate-600">
              Theo quy định kiểm soát chất lượng (FR-018), mọi sự thay đổi lộ trình cần có lý do rõ
              ràng để học viên và giáo vụ theo dõi.
            </p>
            <div>
              <label
                htmlFor="path-revision-reason"
                className="block text-xs font-semibold text-slate-700 mb-1"
              >
                Lý do điều chỉnh <span className="text-red-500">*</span>
              </label>
              <textarea
                id="path-revision-reason"
                maxLength={2000}
                rows={3}
                placeholder="Ví dụ: Học viên tiếp thu nhanh, đẩy nhanh tiến độ Speaking Part 2…"
                value={revisionReason}
                onChange={(e) => setRevisionReason(e.target.value)}
                className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
                required
              />
            </div>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button type="button" variant="outline" onClick={() => setReasonDialogOpen(false)}>
                Hủy
              </Button>
              <Button
                type="button"
                onClick={() => {
                  if (!revisionReason.trim()) {
                    toast.error('Vui lòng nhập lý do điều chỉnh!')
                    return
                  }
                  saveMutation.mutate()
                }}
                disabled={saveMutation.isPending}
              >
                Xác nhận lưu
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
