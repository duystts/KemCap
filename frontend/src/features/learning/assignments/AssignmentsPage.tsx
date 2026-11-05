import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '@/app/AuthProvider'
import {
  fetchAssignments,
  gradeAssignment,
  submitAssignment,
  type AssignmentItem,
  type AssignmentStatus,
} from '@/lib/api/learning'
import { fetchStudents } from '@/lib/api/catalog'
import { ApiError } from '@/lib/api/client'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DateTimeText } from '@/components/DateTimeText'
import { StatusBadge } from '@/components/StatusBadge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'

const MAX_ANSWER = 20000
const MAX_FEEDBACK = 5000

const FILTERS: { value: AssignmentStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'PENDING', label: 'Chưa nộp' },
  { value: 'SUBMITTED', label: 'Đã nộp' },
  { value: 'GRADED', label: 'Đã chấm' },
]

function useRefreshOnConflict() {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()
  return {
    queryClient,
    onError: (err: unknown) => {
      showError(err)
      if (err instanceof ApiError && err.code === 'INVALID_STATE_TRANSITION') {
        queryClient.invalidateQueries({ queryKey: ['assignments'] })
      }
    },
  }
}

function SubmissionForm({ assignment, overdue }: { assignment: AssignmentItem; overdue: boolean }) {
  const { queryClient, onError } = useRefreshOnConflict()
  const [answer, setAnswer] = useState(assignment.submission?.textAnswer ?? '')
  const [editing, setEditing] = useState(!assignment.submission)

  const submitMutation = useMutation({
    mutationFn: () => submitAssignment(assignment.id, answer.trim()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assignments'] })
      toast.success('Đã nộp bài!')
      setEditing(false)
    },
    onError,
  })

  if (!editing) {
    return (
      <Button type="button" variant="outline" size="sm" onClick={() => setEditing(true)}>
        Sửa bài đã nộp
      </Button>
    )
  }

  return (
    <div className="space-y-2">
      <label className="block text-xs font-medium text-slate-700">
        Bài làm
        <textarea
          rows={5}
          maxLength={MAX_ANSWER}
          value={answer}
          onChange={(e) => setAnswer(e.target.value)}
          className="mt-1 w-full px-3 py-2 text-sm border rounded-lg bg-white font-normal"
        />
      </label>
      {overdue && (
        <p className="text-xs text-amber-800">Đã quá hạn: bài nộp bây giờ sẽ ghi là nộp muộn.</p>
      )}
      <div className="flex justify-end gap-2">
        {assignment.submission && (
          <Button type="button" variant="ghost" size="sm" onClick={() => setEditing(false)}>
            Hủy
          </Button>
        )}
        <Button
          type="button"
          size="sm"
          disabled={!answer.trim() || submitMutation.isPending}
          onClick={() => submitMutation.mutate()}
        >
          {submitMutation.isPending ? 'Đang nộp…' : 'Nộp bài'}
        </Button>
      </div>
    </div>
  )
}

function GradeForm({ assignmentId }: { assignmentId: string }) {
  const { queryClient, onError } = useRefreshOnConflict()
  const [score, setScore] = useState('')
  const [feedback, setFeedback] = useState('')
  const [confirmOpen, setConfirmOpen] = useState(false)

  const value = Number(score)
  const valid = score.trim() !== '' && Number.isInteger(value) && value >= 0 && value <= 10

  const gradeMutation = useMutation({
    mutationFn: () => gradeAssignment(assignmentId, value, feedback.trim() || undefined),
    onSuccess: () => {
      setConfirmOpen(false)
      queryClient.invalidateQueries({ queryKey: ['assignments'] })
      toast.success('Đã chấm bài!')
    },
    onError: (err) => {
      setConfirmOpen(false)
      onError(err)
    },
  })

  return (
    <div className="space-y-2 pt-3 border-t">
      <label className="flex items-center gap-2 text-xs font-medium text-slate-700">
        Điểm (0–10)
        <Input
          type="number"
          min={0}
          max={10}
          step={1}
          value={score}
          onChange={(e) => setScore(e.target.value)}
          className="w-24"
        />
      </label>
      {score.trim() !== '' && !valid && (
        <p className="text-xs text-rose-600">Điểm là số nguyên từ 0 đến 10.</p>
      )}
      <label className="block text-xs font-medium text-slate-700">
        Phản hồi (không bắt buộc)
        <textarea
          rows={3}
          maxLength={MAX_FEEDBACK}
          value={feedback}
          onChange={(e) => setFeedback(e.target.value)}
          className="mt-1 w-full px-3 py-2 text-sm border rounded-lg bg-white font-normal"
        />
      </label>
      <div className="flex justify-end">
        <Button type="button" size="sm" disabled={!valid} onClick={() => setConfirmOpen(true)}>
          Chấm bài
        </Button>
      </div>
      <ConfirmDialog
        open={confirmOpen}
        title={`Chấm ${score}/10?`}
        description="Bài đã chấm không chấm lại được và học viên sẽ nhận thông báo."
        confirmLabel="Chấm bài"
        busy={gradeMutation.isPending}
        onConfirm={() => gradeMutation.mutate()}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  )
}

function AssignmentCard({
  assignment,
  role,
  studentName,
  now,
}: {
  assignment: AssignmentItem
  role: 'STUDENT' | 'TEACHER'
  studentName?: string
  now: number
}) {
  const { submission, status } = assignment
  const overdue = !!assignment.dueAt && new Date(assignment.dueAt).getTime() < now

  return (
    <li className="bg-white rounded-xl border p-5 space-y-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h2 className="text-base text-slate-900">{assignment.title}</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            {role === 'TEACHER' && `${studentName ?? 'Không rõ tên'} · `}
            {assignment.dueAt ? (
              <>
                Hạn nộp: <DateTimeText value={assignment.dueAt} />
              </>
            ) : (
              'Không có hạn nộp'
            )}
            {' · '}
            <Link
              to={`/sessions/${assignment.sessionId}`}
              className="text-indigo-700 hover:underline"
            >
              Xem buổi học
            </Link>
          </p>
        </div>
        <div className="flex items-center gap-1.5">
          {submission?.late && (
            <span className="rounded-full px-2.5 py-0.5 text-xs font-medium bg-rose-100 text-rose-800">
              Nộp muộn
            </span>
          )}
          {!submission && overdue && (
            <span className="rounded-full px-2.5 py-0.5 text-xs font-medium bg-rose-100 text-rose-800">
              Quá hạn
            </span>
          )}
          <StatusBadge status={status} />
        </div>
      </div>

      {assignment.instructions && (
        <p className="text-sm text-slate-700 whitespace-pre-wrap">{assignment.instructions}</p>
      )}

      {submission && (
        <div className="p-3 rounded-lg bg-slate-50 border space-y-1">
          <p className="text-xs text-slate-500">
            Bài nộp lúc <DateTimeText value={submission.submittedAt} />
          </p>
          <p className="text-sm text-slate-800 whitespace-pre-wrap">{submission.textAnswer}</p>
        </div>
      )}

      {status === 'GRADED' && submission && (
        <div className="p-3 rounded-lg border space-y-1">
          <p className="text-sm font-semibold text-slate-900">Điểm: {submission.score}/10</p>
          {submission.feedback && (
            <p className="text-sm text-slate-700 whitespace-pre-wrap">{submission.feedback}</p>
          )}
        </div>
      )}

      {role === 'STUDENT' && status !== 'GRADED' && (
        <SubmissionForm assignment={assignment} overdue={overdue} />
      )}
      {role === 'TEACHER' && status === 'SUBMITTED' && <GradeForm assignmentId={assignment.id} />}
    </li>
  )
}

export default function AssignmentsPage() {
  const { me } = useAuth()
  const role = me?.role === 'TEACHER' ? 'TEACHER' : 'STUDENT'

  const [filter, setFilter] = useState<AssignmentStatus | 'ALL'>('ALL')
  const [now] = useState(() => Date.now())

  const {
    data: assignments = [],
    isLoading,
    isError,
  } = useQuery({ queryKey: ['assignments'], queryFn: () => fetchAssignments() })

  // API bài tập không kèm tên học viên
  const { data: students = [] } = useQuery({
    queryKey: ['students', 'assigned'],
    queryFn: () => fetchStudents(),
    enabled: role === 'TEACHER',
  })
  const nameOf = new Map(students.map((s) => [s.id, s.fullName]))

  const visible = assignments.filter((a) => filter === 'ALL' || a.status === filter)

  return (
    <div className="space-y-6 max-w-4xl mx-auto p-4 sm:p-6">
      <div>
        <h1 className="text-2xl tracking-tight text-slate-900">Bài tập</h1>
        <p className="text-sm text-slate-500 mt-1">
          {role === 'TEACHER'
            ? 'Đọc bài học viên đã nộp, chấm điểm 0–10 và gửi phản hồi.'
            : 'Làm bài giáo viên đã giao. Bài chưa chấm vẫn sửa và nộp lại được.'}
        </p>
      </div>

      <div className="flex flex-wrap gap-2" role="group" aria-label="Lọc theo trạng thái">
        {FILTERS.map((f) => {
          const count =
            f.value === 'ALL'
              ? assignments.length
              : assignments.filter((a) => a.status === f.value).length
          return (
            <Button
              key={f.value}
              type="button"
              size="sm"
              variant={filter === f.value ? 'default' : 'outline'}
              aria-pressed={filter === f.value}
              onClick={() => setFilter(f.value)}
            >
              {f.label} ({count})
            </Button>
          )
        })}
      </div>

      {isLoading ? (
        <div className="p-12 text-center text-sm text-slate-400">Đang tải bài tập…</div>
      ) : isError ? (
        <div className="p-12 text-center text-sm text-rose-600">
          Không tải được bài tập. Vui lòng thử lại.
        </div>
      ) : visible.length === 0 ? (
        <div className="p-12 text-center text-sm text-slate-500 bg-white rounded-xl border">
          {assignments.length === 0 ? 'Chưa có bài tập nào.' : 'Không có bài tập ở mục này.'}
        </div>
      ) : (
        <ul className="space-y-3">
          {visible.map((a) => (
            <AssignmentCard
              key={a.id}
              assignment={a}
              role={role}
              studentName={nameOf.get(a.studentId)}
              now={now}
            />
          ))}
        </ul>
      )}
    </div>
  )
}
