import { useEffect, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  fetchLearningPath,
  submitSessionReport,
  type ReportScores,
  type SessionReportDto,
  type SubmitReportRequest,
} from '@/lib/api/learning'
import { ApiError } from '@/lib/api/client'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DateTimeText } from '@/components/DateTimeText'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { SKILLS } from './skills'

const MAX_COMMENT = 5000
const MAX_TITLE = 200
const MAX_ASSIGNMENTS = 20

interface AssignmentDraft {
  title: string
  instructions: string
  dueAt: string // giá trị của input datetime-local
}

interface Draft {
  taught: string[]
  completed: string[]
  scores: ReportScores
  comment: string
  assignments: AssignmentDraft[]
}

const EMPTY_DRAFT: Draft = { taught: [], completed: [], scores: {}, comment: '', assignments: [] }

const draftKey = (sessionId: string) => `session-report-draft:${sessionId}`

/** Phiếu nộp một lần và backend không có nháp, nên nháp chỉ nằm ở trình duyệt. */
function loadDraft(sessionId: string): Draft {
  try {
    const raw = localStorage.getItem(draftKey(sessionId))
    return raw ? { ...EMPTY_DRAFT, ...JSON.parse(raw) } : EMPTY_DRAFT
  } catch {
    return EMPTY_DRAFT
  }
}

function clearDraft(sessionId: string) {
  try {
    localStorage.removeItem(draftKey(sessionId))
  } catch {
    // không lưu được nháp thì bỏ qua, phiếu vẫn nộp được
  }
}

const toggled = (ids: string[], id: string, on: boolean) =>
  on ? [...new Set([...ids, id])] : ids.filter((x) => x !== id)

export default function SessionReportForm({ report }: { report: SessionReportDto }) {
  const { sessionId, studentId } = report
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [draft, setDraft] = useState<Draft>(() => loadDraft(sessionId))
  const [attempted, setAttempted] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)

  useEffect(() => {
    try {
      localStorage.setItem(draftKey(sessionId), JSON.stringify(draft))
    } catch {
      // như trên
    }
  }, [draft, sessionId])

  // Giáo viên đã bị đổi hoặc học viên chưa có lộ trình: không có chủ đề để chọn, vẫn nộp được
  const { data: path, isLoading: loadingPath } = useQuery({
    queryKey: ['learning-path', studentId],
    queryFn: () => fetchLearningPath(studentId),
    retry: false,
  })
  const milestones = path?.milestones ?? []
  const topicIds = new Set(milestones.flatMap((m) => m.topics.map((t) => t.id)))

  const submitMutation = useMutation({
    mutationFn: (data: SubmitReportRequest) => submitSessionReport(sessionId, data),
    onSuccess: () => {
      setConfirmOpen(false)
      clearDraft(sessionId)
      queryClient.invalidateQueries({ queryKey: ['session-report', sessionId] })
      queryClient.invalidateQueries({ queryKey: ['session-reports'] })
      queryClient.invalidateQueries({ queryKey: ['learning-path', studentId] })
      toast.success('Đã nộp phiếu đánh giá!')
    },
    onError: (err) => {
      setConfirmOpen(false)
      showError(err)
      if (err instanceof ApiError && err.code === 'INVALID_STATE_TRANSITION') {
        clearDraft(sessionId)
        queryClient.invalidateQueries({ queryKey: ['session-report', sessionId] })
      }
    },
  })

  const errors: string[] = []
  if (!SKILLS.some((s) => draft.scores[s.key])) errors.push('Cần chấm ít nhất một kỹ năng.')
  if (!draft.comment.trim()) errors.push('Cần nhận xét buổi học.')
  if (draft.assignments.some((a) => !a.title.trim())) errors.push('Bài tập cần có tiêu đề.')

  const submit = () => {
    const completed = draft.completed.filter((id) => topicIds.has(id))
    submitMutation.mutate({
      taughtTopicIds: draft.taught.filter((id) => topicIds.has(id)),
      completedTopicIds: completed,
      scores: draft.scores,
      comment: draft.comment.trim(),
      assignments: draft.assignments.map((a) => ({
        title: a.title.trim(),
        instructions: a.instructions.trim() || undefined,
        dueAt: a.dueAt ? new Date(a.dueAt).toISOString() : undefined,
      })),
    })
  }

  const setAssignment = (index: number, change: Partial<AssignmentDraft>) =>
    setDraft((d) => ({
      ...d,
      assignments: d.assignments.map((a, i) => (i === index ? { ...a, ...change } : a)),
    }))

  return (
    <Card>
      <CardHeader className="pb-3 border-b">
        <CardTitle className="text-base font-bold text-slate-800">
          Phiếu đánh giá buổi học
        </CardTitle>
        <p className={`text-xs mt-0.5 ${report.overdue ? 'text-rose-600' : 'text-slate-500'}`}>
          {report.overdue ? 'Đã quá hạn nộp: ' : 'Hạn nộp: '}
          <DateTimeText value={report.dueAt} />
        </p>
      </CardHeader>
      <CardContent className="pt-5 space-y-6">
        {/* Chủ đề */}
        <section className="space-y-2">
          <h3 className="text-sm font-semibold text-slate-800">Chủ đề trong lộ trình</h3>
          {loadingPath ? (
            <p className="text-xs text-slate-400">Đang tải lộ trình…</p>
          ) : milestones.length === 0 ? (
            <p className="text-xs text-slate-500">
              Không có chủ đề để chọn. Bạn vẫn nộp được phiếu.
            </p>
          ) : (
            milestones.map((m) => (
              <div key={m.id} className="rounded-xl border border-slate-200 divide-y">
                <div className="px-3 py-2 text-xs font-semibold text-slate-600 bg-slate-50 rounded-t-xl">
                  {m.title}
                </div>
                {m.topics.map((t) => (
                  <div
                    key={t.id}
                    className="px-3 py-2 flex flex-wrap items-center justify-between gap-2 text-sm"
                  >
                    <span className="text-slate-800">{t.title}</span>
                    <span className="flex items-center gap-4 text-xs text-slate-600">
                      <label className="flex items-center gap-1.5">
                        <input
                          type="checkbox"
                          aria-label={`Đã dạy: ${t.title}`}
                          checked={draft.taught.includes(t.id) || draft.completed.includes(t.id)}
                          disabled={draft.completed.includes(t.id)}
                          onChange={(e) =>
                            setDraft((d) => ({
                              ...d,
                              taught: toggled(d.taught, t.id, e.target.checked),
                            }))
                          }
                        />
                        Đã dạy
                      </label>
                      {t.status === 'DONE' ? (
                        <span className="font-medium text-emerald-600">Đã hoàn thành trước đó</span>
                      ) : (
                        <label className="flex items-center gap-1.5">
                          <input
                            type="checkbox"
                            aria-label={`Hoàn thành: ${t.title}`}
                            checked={draft.completed.includes(t.id)}
                            onChange={(e) =>
                              setDraft((d) => ({
                                ...d,
                                completed: toggled(d.completed, t.id, e.target.checked),
                              }))
                            }
                          />
                          Hoàn thành
                        </label>
                      )}
                    </span>
                  </div>
                ))}
              </div>
            ))
          )}
        </section>

        {/* Điểm */}
        <section className="space-y-2">
          <h3 className="text-sm font-semibold text-slate-800">
            Điểm kỹ năng (1–5, bấm lại để bỏ trống)
          </h3>
          <div className="space-y-2">
            {SKILLS.map((s) => (
              <div key={s.key} className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-sm text-slate-700">{s.label}</span>
                <div className="flex gap-1" role="group" aria-label={s.label}>
                  {[1, 2, 3, 4, 5].map((n) => {
                    const active = draft.scores[s.key] === n
                    return (
                      <button
                        key={n}
                        type="button"
                        aria-pressed={active}
                        onClick={() =>
                          setDraft((d) => ({
                            ...d,
                            scores: { ...d.scores, [s.key]: active ? null : n },
                          }))
                        }
                        className={`w-9 h-9 rounded-lg border text-sm font-semibold transition-colors ${
                          active
                            ? 'bg-indigo-600 border-indigo-600 text-white'
                            : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                        }`}
                      >
                        {n}
                      </button>
                    )
                  })}
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Nhận xét */}
        <section className="space-y-2">
          <label htmlFor="report-comment" className="block text-sm font-semibold text-slate-800">
            Nhận xét
          </label>
          <textarea
            id="report-comment"
            rows={4}
            maxLength={MAX_COMMENT}
            value={draft.comment}
            onChange={(e) => setDraft((d) => ({ ...d, comment: e.target.value }))}
            placeholder="Học viên làm tốt điều gì, cần luyện thêm gì…"
            className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
          />
        </section>

        {/* Bài tập */}
        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-slate-800">Bài tập về nhà</h3>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={draft.assignments.length >= MAX_ASSIGNMENTS}
              onClick={() =>
                setDraft((d) => ({
                  ...d,
                  assignments: [...d.assignments, { title: '', instructions: '', dueAt: '' }],
                }))
              }
            >
              Thêm bài tập
            </Button>
          </div>
          {draft.assignments.map((a, i) => (
            <div key={i} className="p-3 rounded-xl border border-slate-200 space-y-2">
              <div className="flex items-center gap-2">
                <Input
                  aria-label={`Tiêu đề bài tập ${i + 1}`}
                  placeholder="Tiêu đề bài tập"
                  maxLength={MAX_TITLE}
                  value={a.title}
                  onChange={(e) => setAssignment(i, { title: e.target.value })}
                />
                <button
                  type="button"
                  aria-label={`Xóa bài tập ${i + 1}`}
                  onClick={() =>
                    setDraft((d) => ({
                      ...d,
                      assignments: d.assignments.filter((_, index) => index !== i),
                    }))
                  }
                  className="px-2 py-1 text-xs font-medium rounded-lg text-slate-500 hover:text-rose-600"
                >
                  Xóa
                </button>
              </div>
              <textarea
                aria-label={`Hướng dẫn bài tập ${i + 1}`}
                rows={2}
                maxLength={MAX_COMMENT}
                placeholder="Hướng dẫn (không bắt buộc)"
                value={a.instructions}
                onChange={(e) => setAssignment(i, { instructions: e.target.value })}
                className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
              />
              <label className="flex items-center gap-2 text-xs text-slate-600">
                Hạn nộp (không bắt buộc)
                <input
                  type="datetime-local"
                  value={a.dueAt}
                  onChange={(e) => setAssignment(i, { dueAt: e.target.value })}
                  className="px-2 py-1 border rounded-lg bg-white"
                />
              </label>
            </div>
          ))}
        </section>

        {attempted && errors.length > 0 && (
          <ul role="alert" className="text-sm text-rose-600 list-disc pl-5">
            {errors.map((e) => (
              <li key={e}>{e}</li>
            ))}
          </ul>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t">
          <p className="text-xs text-slate-500">
            Nháp tự lưu trên trình duyệt này. Phiếu chỉ nộp được một lần, không sửa được.
          </p>
          <Button
            type="button"
            disabled={submitMutation.isPending}
            onClick={() => {
              setAttempted(true)
              if (errors.length === 0) setConfirmOpen(true)
            }}
          >
            Nộp phiếu
          </Button>
        </div>
      </CardContent>

      <ConfirmDialog
        open={confirmOpen}
        title="Nộp phiếu đánh giá?"
        description="Sau khi nộp, phiếu không sửa được và học viên sẽ nhận thông báo."
        confirmLabel="Nộp phiếu"
        busy={submitMutation.isPending}
        onConfirm={submit}
        onCancel={() => setConfirmOpen(false)}
      />
    </Card>
  )
}
