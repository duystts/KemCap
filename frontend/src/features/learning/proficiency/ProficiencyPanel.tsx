import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchProficiency, type AssessmentItem } from '@/lib/api/learning'
import AssessmentFormDialog from './AssessmentFormDialog'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Award, Plus, History, AlertCircle } from 'lucide-react'

interface ProficiencyPanelProps {
  studentId: string
  canEdit?: boolean
}

const CEFR_COLORS: Record<string, string> = {
  A1: 'bg-slate-100 text-slate-800 border-slate-300',
  A2: 'bg-blue-50 text-blue-800 border-blue-200',
  B1: 'bg-emerald-50 text-emerald-800 border-emerald-200',
  B2: 'bg-indigo-50 text-indigo-800 border-indigo-200',
  C1: 'bg-purple-50 text-purple-800 border-purple-200',
  C2: 'bg-rose-50 text-rose-800 border-rose-200',
}

export default function ProficiencyPanel({ studentId, canEdit = true }: ProficiencyPanelProps) {
  const [dialogOpen, setDialogOpen] = useState(false)

  const { data: prof, isLoading } = useQuery({
    queryKey: ['proficiency', studentId],
    queryFn: () => fetchProficiency(studentId),
  })

  const current = prof?.current
  const history = prof?.history || []

  const renderSkillCard = (name: string, level?: string) => {
    const lvl = level || '—'
    const color = CEFR_COLORS[lvl] || 'bg-slate-50 text-slate-600 border-slate-200'
    return (
      <div className="bg-white p-3.5 rounded-xl border border-slate-200 flex flex-col items-center justify-center text-center shadow-2xs">
        <span className="text-xs font-medium text-slate-500 mb-1">{name}</span>
        <span className={`px-3 py-1 rounded-lg text-base font-black border ${color}`}>{lvl}</span>
      </div>
    )
  }

  if (isLoading) {
    return <div className="p-8 text-center text-sm text-slate-400">Đang tải hồ sơ năng lực…</div>
  }

  return (
    <div className="space-y-6">
      {/* Current Proficiency Card */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-3">
          <div className="flex items-center gap-2">
            <Award className="w-5 h-5 text-indigo-600" />
            <CardTitle className="text-base font-bold text-slate-800">
              Năng lực hiện tại (CEFR 5 kỹ năng)
            </CardTitle>
          </div>
          {canEdit && (
            <Button size="sm" onClick={() => setDialogOpen(true)}>
              <Plus className="w-4 h-4 mr-1.5" />
              Đánh giá mới
            </Button>
          )}
        </CardHeader>
        <CardContent>
          {!current ? (
            <div className="p-8 text-center bg-slate-50 border border-dashed rounded-xl">
              <AlertCircle className="w-6 h-6 text-amber-600 mx-auto mb-2" />
              <p className="text-sm font-semibold text-slate-700">
                Chưa có kết quả đánh giá năng lực
              </p>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                Học viên cần hoàn thành bài kiểm tra đầu vào (Input Test) để xác định trình độ và
                thiết lập lộ trình học phù hợp.
              </p>
            </div>
          ) : (
            <div className="space-y-5">
              <div className="flex flex-wrap items-center justify-between gap-3 p-3 bg-indigo-50/50 rounded-xl border border-indigo-100 text-xs text-indigo-900">
                <div className="flex items-center gap-2">
                  <span className="font-semibold uppercase tracking-wider text-[11px] bg-indigo-100 text-indigo-800 px-2.5 py-0.5 rounded-full">
                    {current.type === 'INPUT' ? 'Đầu vào' : 'Định kỳ'}
                  </span>
                  <span>
                    Đánh giá ngày:{' '}
                    <strong>{new Date(current.assessedAt).toLocaleDateString('vi-VN')}</strong>
                  </span>
                </div>
                <div className="flex items-center gap-1.5 font-bold text-sm">
                  <span>Trình độ chung:</span>
                  <span className="text-base font-black text-indigo-600">
                    CEFR {current.overall}
                  </span>
                </div>
              </div>

              {/* 5 skills grid */}
              <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                {renderSkillCard('Nghe (Listening)', current.listening)}
                {renderSkillCard('Nói (Speaking)', current.speaking)}
                {renderSkillCard('Đọc (Reading)', current.reading)}
                {renderSkillCard('Viết (Writing)', current.writing)}
                {renderSkillCard('Ngữ pháp & Từ vựng', current.grammarVocab)}
              </div>

              {current.notes && (
                <div className="p-3.5 bg-slate-50 rounded-xl border text-xs text-slate-700">
                  <strong className="text-slate-800 block mb-1">Nhận xét của giáo viên:</strong>
                  {current.notes}
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* History */}
      {history.length > 0 && (
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center gap-2">
              <History className="w-5 h-5 text-slate-500" />
              <CardTitle className="text-base font-bold text-slate-800">
                Lịch sử đánh giá năng lực ({history.length})
              </CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            {history.map((h: AssessmentItem) => (
              <div
                key={h.id}
                className="p-3.5 rounded-xl border border-slate-100 bg-slate-50/50 text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-semibold text-slate-800">
                      {h.type === 'INPUT' ? 'Đánh giá đầu vào' : 'Đánh giá định kỳ'}
                    </span>
                    <span className="text-slate-400">•</span>
                    <span className="text-slate-500">
                      {new Date(h.assessedAt).toLocaleDateString('vi-VN')}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-2 text-slate-600">
                    <span>
                      L: <strong>{h.listening}</strong>
                    </span>
                    <span>
                      S: <strong>{h.speaking}</strong>
                    </span>
                    <span>
                      R: <strong>{h.reading}</strong>
                    </span>
                    <span>
                      W: <strong>{h.writing}</strong>
                    </span>
                    <span>
                      Gr/Vocab: <strong>{h.grammarVocab}</strong>
                    </span>
                  </div>
                  {h.notes && (
                    <div className="text-slate-500 mt-1 italic">&quot;{h.notes}&quot;</div>
                  )}
                </div>
                <div className="shrink-0">
                  <span className="inline-flex items-center px-2.5 py-1 rounded-lg font-black text-sm bg-indigo-100 text-indigo-800">
                    {h.overall}
                  </span>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Assessment Form Dialog */}
      <AssessmentFormDialog
        studentId={studentId}
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
      />
    </div>
  )
}
