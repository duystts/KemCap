import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { createAssessment, type AssessmentType } from '@/lib/api/learning'
import type { CefrLevel } from '@/lib/api/catalog'
import { Button } from '@/components/ui/button'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { X, Award, CheckCircle } from 'lucide-react'

interface AssessmentFormDialogProps {
  studentId: string
  open: boolean
  onClose: () => void
}

const CEFR_LEVELS: CefrLevel[] = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']

export default function AssessmentFormDialog({
  studentId,
  open,
  onClose,
}: AssessmentFormDialogProps) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [type, setType] = useState<AssessmentType>('INPUT')
  const [listening, setListening] = useState<CefrLevel>('B1')
  const [speaking, setSpeaking] = useState<CefrLevel>('B1')
  const [reading, setReading] = useState<CefrLevel>('B1')
  const [writing, setWriting] = useState<CefrLevel>('B1')
  const [grammarVocab, setGrammarVocab] = useState<CefrLevel>('B1')
  const [notes, setNotes] = useState('')

  const createMutation = useMutation({
    mutationFn: () =>
      createAssessment(studentId, {
        type,
        listening,
        speaking,
        reading,
        writing,
        grammarVocab,
        notes: notes.trim() || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['proficiency', studentId] })
      queryClient.invalidateQueries({ queryKey: ['students'] })
      toast.success('Đã lưu kết quả đánh giá năng lực CEFR thành công!')
      onClose()
    },
    onError: (err) => showError(err),
  })

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div className="p-5 border-b flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Award className="w-5 h-5 text-indigo-600" />
            <h2 className="text-lg font-bold text-slate-800">Đánh giá năng lực CEFR 5 kỹ năng</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault()
            createMutation.mutate()
          }}
          className="p-6 space-y-4"
        >
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Hình thức đánh giá <span className="text-red-500">*</span>
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as AssessmentType)}
              className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
            >
              <option value="INPUT">Đánh giá đầu vào (Input Test)</option>
              <option value="PERIODIC">Đánh giá định kỳ (Periodic Test)</option>
            </select>
          </div>

          <div className="space-y-3 pt-2 border-t">
            <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Khung năng lực 5 kỹ năng (CEFR A1 – C2)
            </span>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Kỹ năng Nghe (Listening)
                </label>
                <select
                  value={listening}
                  onChange={(e) => setListening(e.target.value as CefrLevel)}
                  className="w-full px-3 py-1.5 text-sm border rounded-lg bg-white"
                >
                  {CEFR_LEVELS.map((lvl) => (
                    <option key={lvl} value={lvl}>
                      {lvl}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Kỹ năng Nói (Speaking)
                </label>
                <select
                  value={speaking}
                  onChange={(e) => setSpeaking(e.target.value as CefrLevel)}
                  className="w-full px-3 py-1.5 text-sm border rounded-lg bg-white"
                >
                  {CEFR_LEVELS.map((lvl) => (
                    <option key={lvl} value={lvl}>
                      {lvl}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Kỹ năng Đọc (Reading)
                </label>
                <select
                  value={reading}
                  onChange={(e) => setReading(e.target.value as CefrLevel)}
                  className="w-full px-3 py-1.5 text-sm border rounded-lg bg-white"
                >
                  {CEFR_LEVELS.map((lvl) => (
                    <option key={lvl} value={lvl}>
                      {lvl}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Kỹ năng Viết (Writing)
                </label>
                <select
                  value={writing}
                  onChange={(e) => setWriting(e.target.value as CefrLevel)}
                  className="w-full px-3 py-1.5 text-sm border rounded-lg bg-white"
                >
                  {CEFR_LEVELS.map((lvl) => (
                    <option key={lvl} value={lvl}>
                      {lvl}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Ngữ pháp & Từ vựng (Grammar & Vocabulary)
              </label>
              <select
                value={grammarVocab}
                onChange={(e) => setGrammarVocab(e.target.value as CefrLevel)}
                className="w-full px-3 py-1.5 text-sm border rounded-lg bg-white"
              >
                {CEFR_LEVELS.map((lvl) => (
                  <option key={lvl} value={lvl}>
                    {lvl}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Nhận xét chi tiết của giáo viên
            </label>
            <textarea
              rows={3}
              placeholder="Điểm mạnh, điểm cần cải thiện, lỗi phát âm hoặc ngữ pháp trọng tâm…"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
            />
          </div>

          <div className="pt-4 border-t flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" disabled={createMutation.isPending}>
              <CheckCircle className="w-4 h-4 mr-1.5" />
              {createMutation.isPending ? 'Đang lưu…' : 'Lưu đánh giá'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
