import { useQuery } from '@tanstack/react-query'
import { fetchPathRevisions, type RevisionItem } from '@/lib/api/learning'
import { X, History, User, Calendar } from 'lucide-react'

interface RevisionHistoryProps {
  pathId: string
  open: boolean
  onClose: () => void
}

export default function RevisionHistory({ pathId, open, onClose }: RevisionHistoryProps) {
  const { data: revisions = [], isLoading } = useQuery({
    queryKey: ['learning-path-revisions', pathId],
    queryFn: () => fetchPathRevisions(pathId),
    enabled: open && !!pathId,
  })

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex justify-end">
      <div className="bg-white w-full max-w-md h-full shadow-2xl flex flex-col justify-between overflow-y-auto animate-in slide-in-from-right duration-200">
        <div>
          <div className="p-5 border-b flex items-center justify-between">
            <div className="flex items-center gap-2">
              <History className="w-5 h-5 text-indigo-600" />
              <h2 className="text-base font-bold text-slate-800">Lịch sử điều chỉnh lộ trình</h2>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="p-5 space-y-4">
            {isLoading ? (
              <div className="p-8 text-center text-sm text-slate-400">Đang tải lịch sử…</div>
            ) : revisions.length === 0 ? (
              <div className="p-8 text-center text-sm text-slate-500 bg-slate-50 rounded-xl border">
                Chưa có lần điều chỉnh nào được ghi lại.
              </div>
            ) : (
              <div className="space-y-4 relative before:absolute before:inset-0 before:left-3.5 before:w-0.5 before:bg-slate-200">
                {revisions.map((rev: RevisionItem) => (
                  <div key={rev.id} className="relative flex items-start gap-4 pl-8">
                    <div className="absolute left-2 top-1.5 w-3.5 h-3.5 rounded-full bg-indigo-600 ring-4 ring-white" />
                    <div className="flex-1 bg-slate-50 p-3.5 rounded-xl border border-slate-200 text-xs space-y-1.5">
                      <div className="flex items-center justify-between text-slate-500">
                        <span className="flex items-center gap-1 font-semibold text-slate-700">
                          <User className="w-3.5 h-3.5" />
                          Người đổi: {rev.changedBy}
                        </span>
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3.5 h-3.5" />
                          {new Date(rev.changedAt).toLocaleDateString('vi-VN')}
                        </span>
                      </div>
                      <div className="font-semibold text-slate-800 text-sm">
                        Lý do: &quot;{rev.reason}&quot;
                      </div>
                      <div className="text-slate-600">{rev.summary}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="p-4 border-t bg-slate-50 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-xs font-semibold rounded-lg bg-white border border-slate-300 text-slate-700 hover:bg-slate-100"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  )
}
