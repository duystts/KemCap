import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchLearningPath, type MilestoneItem, type TopicItem } from '@/lib/api/learning'
import LearningPathEditor from './LearningPathEditor'
import RevisionHistory from './RevisionHistory'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { BookOpen, Edit, History, CheckCircle2, Clock, Circle, Calendar, Plus } from 'lucide-react'

interface LearningPathViewProps {
  studentId: string
  canEdit?: boolean
}

export default function LearningPathView({ studentId, canEdit = true }: LearningPathViewProps) {
  const [editorOpen, setEditorOpen] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)

  const { data: path, isLoading } = useQuery({
    queryKey: ['learning-path', studentId],
    queryFn: () => fetchLearningPath(studentId),
  })

  if (isLoading) {
    return <div className="p-8 text-center text-sm text-slate-400">Đang tải lộ trình học…</div>
  }

  if (!path) {
    return (
      <Card>
        <CardContent className="p-8 text-center bg-slate-50 border border-dashed rounded-xl">
          <BookOpen className="w-8 h-8 text-slate-400 mx-auto mb-2" />
          <h3 className="font-semibold text-slate-800">Chưa có lộ trình học tập cá nhân</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto mb-4">
            Lộ trình học tập chi tiết sẽ được giáo viên thiết kế dựa trên kết quả đánh giá năng lực
            đầu vào.
          </p>
          {canEdit && (
            <Button onClick={() => setEditorOpen(true)}>
              <Plus className="w-4 h-4 mr-1.5" />
              Thiết kế lộ trình ngay
            </Button>
          )}

          <LearningPathEditor
            studentId={studentId}
            existingPath={null}
            open={editorOpen}
            onClose={() => setEditorOpen(false)}
          />
        </CardContent>
      </Card>
    )
  }

  const renderTopicStatus = (status: TopicItem['status']) => {
    switch (status) {
      case 'DONE':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
            <CheckCircle2 className="w-3.5 h-3.5" /> Hoàn thành
          </span>
        )
      case 'IN_PROGRESS':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full">
            <Clock className="w-3.5 h-3.5" /> Đang học
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-medium text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
            <Circle className="w-3 h-3" /> Chưa học
          </span>
        )
    }
  }

  return (
    <div className="space-y-6">
      {/* Path Header Card */}
      <Card>
        <CardContent className="p-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b">
            <div>
              <div className="text-xs font-bold uppercase tracking-wider text-indigo-600 mb-1">
                Lộ trình học tập cá nhân hóa
              </div>
              <h2 className="text-xl font-bold text-slate-900">{path.title}</h2>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setHistoryOpen(true)}
                className="text-xs"
              >
                <History className="w-4 h-4 mr-1.5 text-slate-500" />
                Lịch sử điều chỉnh
              </Button>
              {canEdit && (
                <Button size="sm" onClick={() => setEditorOpen(true)} className="text-xs">
                  <Edit className="w-4 h-4 mr-1.5" />
                  Điều chỉnh lộ trình
                </Button>
              )}
            </div>
          </div>

          {/* Progress Bar */}
          <div className="pt-4 space-y-2">
            <div className="flex items-center justify-between text-xs font-semibold">
              <span className="text-slate-600">Tiến độ hoàn thành lộ trình</span>
              <span className="text-indigo-600 font-bold">{path.progressPercent || 0}%</span>
            </div>
            <div className="w-full h-3 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-linear-to-r from-indigo-500 to-indigo-600 rounded-full transition-all duration-500"
                style={{ width: `${Math.min(100, Math.max(0, path.progressPercent || 0))}%` }}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Milestones & Topics Timeline */}
      <div className="space-y-6">
        {path.milestones.map((m: MilestoneItem, mIdx: number) => (
          <div
            key={m.id}
            className="bg-white rounded-2xl border border-slate-200 p-5 sm:p-6 shadow-2xs space-y-4"
          >
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b">
              <div className="flex items-center gap-3">
                <span className="w-7 h-7 rounded-full bg-indigo-600 text-white font-bold text-xs flex items-center justify-center shrink-0">
                  {mIdx + 1}
                </span>
                <h3 className="font-bold text-slate-900 text-base">{m.title}</h3>
              </div>
              {m.targetDate && (
                <span className="text-xs text-slate-500 flex items-center gap-1.5 self-start sm:self-auto bg-slate-50 px-2.5 py-1 rounded-lg border">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  Mục tiêu: {new Date(m.targetDate).toLocaleDateString('vi-VN')}
                </span>
              )}
            </div>

            {/* Topics List */}
            <div className="space-y-2.5 pl-0 sm:pl-10">
              {m.topics.length === 0 ? (
                <div className="text-xs text-slate-400 italic">
                  Chưa có chủ đề nào trong chặng này.
                </div>
              ) : (
                m.topics.map((t: TopicItem, tIdx: number) => (
                  <div
                    key={t.id}
                    className="p-3 bg-slate-50 rounded-xl border border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2 hover:bg-white hover:border-indigo-100 transition-colors"
                  >
                    <div className="flex items-start gap-2.5">
                      <span className="text-xs font-semibold text-slate-400 mt-0.5">
                        {mIdx + 1}.{tIdx + 1}
                      </span>
                      <div>
                        <div className="text-sm font-semibold text-slate-800">{t.title}</div>
                        {t.objective && (
                          <div className="text-xs text-slate-500 mt-0.5">{t.objective}</div>
                        )}
                      </div>
                    </div>
                    <div className="self-end sm:self-auto shrink-0">
                      {renderTopicStatus(t.status)}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Dialogs */}
      <LearningPathEditor
        studentId={studentId}
        existingPath={path}
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
      />
      <RevisionHistory pathId={path.id} open={historyOpen} onClose={() => setHistoryOpen(false)} />
    </div>
  )
}
