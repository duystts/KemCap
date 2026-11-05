import { useCallback, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { toast } from 'sonner'
import { LiveKitRoom } from '@livekit/components-react'
import { DisconnectReason } from 'livekit-client'
import '@livekit/components-styles'
import { useAuth } from '@/app/AuthProvider'
import { joinClassroom, type JoinInfo } from '@/lib/api/scheduling'
import SessionOutcomeDialog from '@/features/scheduling/SessionOutcomeDialog'
import { Button } from '@/components/ui/button'
import { errorMessage } from '@/components/useApiErrorToast'
import ClassroomRoom from './ClassroomRoom'

export default function ClassroomPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { me } = useAuth()
  const isTeacher = me?.role === 'TEACHER'
  const schedulePath = isTeacher ? '/teacher/schedule' : '/student/schedule'

  const [outcomeDialogOpen, setOutcomeDialogOpen] = useState(false)
  const [connectionLost, setConnectionLost] = useState(false)

  const {
    data: joinInfo,
    isLoading,
    error,
  } = useQuery<JoinInfo>({
    queryKey: ['join-room', id],
    queryFn: () => joinClassroom(id!),
    enabled: !!id,
    // Mỗi lần vào lại lấy token mới
    gcTime: 0,
    staleTime: 0,
  })

  const handleDisconnected = useCallback(
    (reason?: DisconnectReason) => {
      // Mất mạng hoặc bị ngắt từ máy chủ không phải là rời phòng: chưa được hỏi kết quả buổi học
      if (reason !== DisconnectReason.CLIENT_INITIATED) {
        setConnectionLost(true)
        return
      }
      if (isTeacher) {
        setOutcomeDialogOpen(true)
      } else {
        navigate(schedulePath)
      }
    },
    [isTeacher, navigate, schedulePath],
  )

  if (isLoading) {
    return (
      <div className="h-screen w-screen bg-slate-950 text-white flex flex-col items-center justify-center p-4">
        <h2 className="text-lg">Đang chuẩn bị phòng học…</h2>
      </div>
    )
  }

  if (error || !joinInfo) {
    return (
      <div className="h-screen w-screen bg-slate-950 text-white flex flex-col items-center justify-center p-4 text-center">
        <h2 className="text-xl">Không vào được phòng học</h2>
        <p className="text-sm text-slate-300 max-w-md mt-2 mb-6">{errorMessage(error)}</p>
        <Button onClick={() => navigate(schedulePath)}>Quay lại lịch học</Button>
      </div>
    )
  }

  if (connectionLost) {
    return (
      <div className="h-screen w-screen bg-slate-950 text-white flex flex-col items-center justify-center p-4 text-center">
        <h2 className="text-xl">Mất kết nối phòng học</h2>
        <p className="text-sm text-slate-300 max-w-md mt-2 mb-6">
          Kiểm tra mạng rồi vào lại. Buổi học chưa được ghi nhận kết quả.
        </p>
        <div className="flex gap-2">
          <Button
            variant="outline"
            className="text-slate-900"
            onClick={() => navigate(schedulePath)}
          >
            Quay lại lịch học
          </Button>
          <Button onClick={() => window.location.reload()}>Vào lại phòng</Button>
        </div>
      </div>
    )
  }

  return (
    <div className="h-screen w-screen bg-slate-950 text-white flex flex-col overflow-hidden relative">
      <div className="h-12 border-b border-slate-800 bg-slate-900 px-4 flex items-center justify-between shrink-0 text-xs">
        <span className="font-semibold text-slate-200">Lớp học trực tuyến 1-1</span>
        <span className="text-slate-300 truncate ml-3">
          {me?.fullName || me?.email} ({isTeacher ? 'Giáo viên' : 'Học viên'})
        </span>
      </div>

      <div className="flex-1 relative overflow-hidden">
        <LiveKitRoom
          video
          audio
          token={joinInfo.token}
          serverUrl={joinInfo.serverUrl}
          connect
          onDisconnected={handleDisconnected}
          onMediaDeviceFailure={() =>
            toast.error('Không dùng được micro hoặc camera. Hãy kiểm tra quyền của trình duyệt.')
          }
          data-lk-theme="default"
          className="h-full w-full"
        >
          <ClassroomRoom />
        </LiveKitRoom>
      </div>

      {id && (
        <SessionOutcomeDialog
          sessionId={id}
          open={outcomeDialogOpen}
          onClose={() => {
            setOutcomeDialogOpen(false)
            navigate(schedulePath)
          }}
          onSuccess={() => navigate(schedulePath)}
        />
      )}
    </div>
  )
}
