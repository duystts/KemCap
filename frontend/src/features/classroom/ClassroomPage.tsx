import { useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/app/AuthProvider'
import { joinClassroom, type JoinInfo } from '@/lib/api/scheduling'
import SessionOutcomeDialog from '@/features/scheduling/SessionOutcomeDialog'
import { Button } from '@/components/ui/button'
import { AlertCircle, ArrowLeft } from 'lucide-react'
import { LiveKitRoom, VideoConference, RoomAudioRenderer } from '@livekit/components-react'
import '@livekit/components-styles'

export default function ClassroomPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { me } = useAuth()
  const isTeacher = me?.role === 'TEACHER'

  const [outcomeDialogOpen, setOutcomeDialogOpen] = useState(false)

  // Gọi API /sessions/{id}/join để lấy room token và server url
  const {
    data: joinInfo,
    isLoading,
    error,
  } = useQuery<JoinInfo>({
    queryKey: ['join-room', id],
    queryFn: () => joinClassroom(id!),
    enabled: !!id,
    retry: 1,
  })

  const handleDisconnected = useCallback(() => {
    if (isTeacher) {
      setOutcomeDialogOpen(true)
    } else {
      navigate('/student/schedule')
    }
  }, [isTeacher, navigate])

  if (isLoading) {
    return (
      <div className="h-screen w-screen bg-slate-950 text-white flex flex-col items-center justify-center p-4">
        <div className="w-12 h-12 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin mb-4"></div>
        <h2 className="text-lg font-semibold">Đang chuẩn bị phòng học trực tuyến…</h2>
        <p className="text-xs text-slate-400 mt-1">
          Đang cấp quyền và kết nối máy chủ LiveKit WebRTC
        </p>
      </div>
    )
  }

  if (error || !joinInfo) {
    return (
      <div className="h-screen w-screen bg-slate-950 text-white flex flex-col items-center justify-center p-4 text-center">
        <div className="p-4 rounded-full bg-red-500/10 text-red-500 mb-4">
          <AlertCircle className="w-10 h-10" />
        </div>
        <h2 className="text-xl font-bold">Không thể tham gia phòng học</h2>
        <p className="text-sm text-slate-400 max-w-md mt-2 mb-6">
          Phòng học có thể chưa tới giờ mở (chỉ mở trước 10 phút), hoặc buổi học đã kết thúc/bị hủy.
        </p>
        <Button
          onClick={() => navigate(-1)}
          className="bg-indigo-600 hover:bg-indigo-700 text-white font-medium flex items-center gap-2"
        >
          <ArrowLeft className="w-4 h-4" /> Quay lại lịch học
        </Button>
      </div>
    )
  }

  return (
    <div className="h-screen w-screen bg-slate-950 text-white flex flex-col overflow-hidden relative">
      {/* Thanh tiêu đề phòng học tối giản */}
      <div className="h-12 border-b border-slate-800 bg-slate-900/80 backdrop-blur-md px-4 flex items-center justify-between z-10 shrink-0">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-300">
              Lớp học trực tuyến 1-1
            </span>
          </div>
          <span className="text-slate-600">|</span>
          <span className="text-xs text-slate-400 font-mono">Phòng: {joinInfo.roomName}</span>
        </div>

        <div className="flex items-center gap-3 text-xs">
          <span className="text-slate-400">
            Đang tham gia: <strong className="text-slate-200">{me?.fullName || me?.email}</strong> (
            {isTeacher ? 'Giáo viên' : 'Học viên'})
          </span>
          <button
            type="button"
            onClick={handleDisconnected}
            className="px-3 py-1 rounded bg-rose-600/80 hover:bg-rose-600 text-white font-medium transition-colors cursor-pointer"
          >
            Rời phòng
          </button>
        </div>
      </div>

      {/* Khu vực Video LiveKit */}
      <div className="flex-1 relative overflow-hidden">
        <LiveKitRoom
          video={true}
          audio={true}
          token={joinInfo.token}
          serverUrl={joinInfo.serverUrl}
          connect={true}
          onDisconnected={handleDisconnected}
          data-lk-theme="default"
          className="h-full w-full"
        >
          <VideoConference />
          <RoomAudioRenderer />
        </LiveKitRoom>
      </div>

      {/* Dialog chốt kết quả khi Giáo viên rời phòng */}
      {id && (
        <SessionOutcomeDialog
          sessionId={id}
          open={outcomeDialogOpen}
          onClose={() => {
            setOutcomeDialogOpen(false)
            navigate('/teacher/schedule')
          }}
          onSuccess={() => {
            navigate('/teacher/schedule')
          }}
        />
      )}
    </div>
  )
}
