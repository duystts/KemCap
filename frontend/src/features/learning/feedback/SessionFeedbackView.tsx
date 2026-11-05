import { useQuery } from '@tanstack/react-query'
import { fetchSessionFeedback } from '@/lib/api/learning'

/** Giáo vụ xem đánh giá của học viên; giáo viên không có quyền (FR-022). */
export default function SessionFeedbackView({ sessionId }: { sessionId: string }) {
  const { data: feedback, isLoading } = useQuery({
    queryKey: ['session-feedback', sessionId],
    queryFn: () => fetchSessionFeedback(sessionId),
    retry: false,
  })

  if (isLoading) return null

  return (
    <div className="bg-white rounded-xl border p-5 space-y-1">
      <h2 className="text-base text-slate-900">Đánh giá của học viên</h2>
      {feedback ? (
        <>
          <span aria-label={`${feedback.stars} sao`} className="text-indigo-600">
            {'★'.repeat(feedback.stars)}
            <span className="text-slate-300">{'★'.repeat(5 - feedback.stars)}</span>
          </span>
          {feedback.comment && (
            <p className="text-sm text-slate-700 whitespace-pre-wrap">{feedback.comment}</p>
          )}
        </>
      ) : (
        <p className="text-sm text-slate-500">Học viên chưa đánh giá buổi này.</p>
      )}
    </div>
  )
}
