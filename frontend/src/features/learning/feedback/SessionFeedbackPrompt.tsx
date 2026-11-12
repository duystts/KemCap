import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { fetchMyFeedback, submitSessionFeedback } from '@/lib/api/learning'
import type { SessionItem } from '@/lib/api/scheduling'
import { ApiError } from '@/lib/api/client'
import { DateTimeText } from '@/components/DateTimeText'
import { Button } from '@/components/ui/button'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { canRate } from './canRate'

const MAX_COMMENT = 2000

/**
 * Form chấm sao một buổi, không bắt buộc. Đã gửi thì hiện lại đánh giá của chính học viên (chỉ đọc);
 * quá hạn mà chưa gửi thì ẩn.
 */
export default function SessionFeedbackPrompt({ session }: { session: SessionItem }) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [stars, setStars] = useState(0)
  const [comment, setComment] = useState('')
  const [closed, setClosed] = useState(false)
  const [now] = useState(() => Date.now())

  const { data: mine, isLoading } = useQuery({
    queryKey: ['session-feedback', 'mine'],
    queryFn: fetchMyFeedback,
    enabled: session.status === 'COMPLETED',
  })

  const submitMutation = useMutation({
    mutationFn: () => submitSessionFeedback(session.id, stars, comment.trim() || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['session-feedback', 'mine'] })
      toast.success('Cảm ơn bạn đã đánh giá buổi học!')
    },
    onError: (err) => {
      showError(err)
      if (err instanceof ApiError && err.code === 'FEEDBACK_EXISTS') {
        queryClient.invalidateQueries({ queryKey: ['session-feedback', 'mine'] })
      }
      if (err instanceof ApiError && err.code === 'FEEDBACK_WINDOW_CLOSED') setClosed(true)
    },
  })

  if (isLoading) return null

  const given = mine?.find((f) => f.sessionId === session.id)
  if (given) {
    return (
      <div className="bg-white rounded-xl border p-5 space-y-1">
        <h2 className="text-base text-slate-900">Đánh giá của bạn</h2>
        <span aria-label={`${given.stars} sao`} className="text-indigo-600">
          {'★'.repeat(given.stars)}
          <span className="text-slate-300">{'★'.repeat(5 - given.stars)}</span>
        </span>
        {given.comment && (
          <p className="text-sm text-slate-700 whitespace-pre-wrap">{given.comment}</p>
        )}
        <p className="text-xs text-slate-500">
          Gửi <DateTimeText value={given.createdAt} />. Chỉ bạn và giáo vụ xem được.
        </p>
      </div>
    )
  }

  if (closed || !canRate(session, now)) return null

  return (
    <div className="bg-white rounded-xl border p-5 space-y-3">
      <div>
        <h2 className="text-base text-slate-900">Đánh giá buổi học</h2>
        <p className="text-xs text-slate-500 mt-0.5">
          Buổi <DateTimeText value={session.startsAt} />. Không bắt buộc, chỉ giáo vụ xem được.
        </p>
      </div>
      <div className="flex gap-1" role="group" aria-label="Số sao">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            type="button"
            aria-label={`${n} sao`}
            aria-pressed={stars === n}
            onClick={() => setStars(n)}
            className={`w-9 h-9 text-xl leading-none rounded-lg transition-colors hover:bg-slate-100 ${
              n <= stars ? 'text-indigo-600' : 'text-slate-300'
            }`}
          >
            ★
          </button>
        ))}
      </div>
      <label className="block text-xs font-medium text-slate-700">
        Nhận xét (không bắt buộc)
        <textarea
          rows={2}
          maxLength={MAX_COMMENT}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          className="mt-1 w-full px-3 py-2 text-sm border rounded-lg bg-white font-normal"
        />
      </label>
      <div className="flex justify-end">
        <Button
          type="button"
          size="sm"
          disabled={stars === 0 || submitMutation.isPending}
          onClick={() => submitMutation.mutate()}
        >
          {submitMutation.isPending ? 'Đang gửi…' : 'Gửi đánh giá'}
        </Button>
      </div>
    </div>
  )
}
