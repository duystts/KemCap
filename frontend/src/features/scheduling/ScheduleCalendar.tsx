import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import { useAuth } from '@/app/AuthProvider'
import { fetchSessions, type SessionStatus } from '@/lib/api/scheduling'
import SessionDetailDrawer from './SessionDetailDrawer'
import { statusLabel } from '@/components/StatusBadge'
import { Card, CardContent } from '@/components/ui/card'
import { Calendar, Filter } from 'lucide-react'

// Màu sắc cho 8 trạng thái buổi học
const STATUS_COLORS: Record<SessionStatus, { bg: string; border: string; text: string }> = {
  BOOKED: { bg: '#dbeafe', border: '#3b82f6', text: '#1e40af' },
  COMPLETED: { bg: '#d1fae5', border: '#10b981', text: '#065f46' },
  STUDENT_NO_SHOW: { bg: '#fee2e2', border: '#ef4444', text: '#991b1b' },
  TEACHER_ABSENT: { bg: '#fef3c7', border: '#f59e0b', text: '#92400e' },
  CANCELLED_ON_TIME: { bg: '#f1f5f9', border: '#94a3b8', text: '#475569' },
  CANCELLED_LATE: { bg: '#ffe4e6', border: '#e11d48', text: '#9f1239' },
  CANCELLED_SYSTEM: { bg: '#f1f5f9', border: '#64748b', text: '#334155' },
  INTERRUPTED: { bg: '#fef9c3', border: '#eab308', text: '#854d0e' },
}

export default function ScheduleCalendar() {
  const { me } = useAuth()
  const isStudent = me?.role === 'STUDENT'
  const isTeacher = me?.role === 'TEACHER'

  const [outcomePendingOnly, setOutcomePendingOnly] = useState(false)
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null)
  const [drawerOpen, setDrawerOpen] = useState(false)

  // Fetch danh sách buổi học
  const { data: sessions = [], isLoading } = useQuery({
    queryKey: ['my-sessions', me?.id, outcomePendingOnly],
    queryFn: () =>
      fetchSessions({
        teacherId: isTeacher ? me?.id : undefined,
        studentId: isStudent ? me?.id : undefined,
        outcomePending: outcomePendingOnly ? true : undefined,
      }),
    enabled: !!me?.id,
  })

  const calendarEvents = sessions.map((s) => {
    const style = STATUS_COLORS[s.status] || STATUS_COLORS.BOOKED
    return {
      id: s.id,
      title: statusLabel(s.status),
      start: s.startsAt,
      end: s.endsAt,
      backgroundColor: style.bg,
      borderColor: style.border,
      textColor: style.text,
      extendedProps: s,
    }
  })

  const handleEventClick = (info: any) => {
    setSelectedSessionId(info.event.id)
    setDrawerOpen(true)
  }

  return (
    <div className="space-y-6 max-w-6xl mx-auto p-4 sm:p-6">
      {/* Header & Bộ lọc */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Calendar className="w-6 h-6 text-indigo-600" />
            {isTeacher
              ? 'Lịch giảng dạy 1 kèm 1'
              : isStudent
                ? 'Lịch học cá nhân'
                : 'Quản lý Lịch toàn hệ thống'}
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Hiển thị theo múi giờ: <strong>{me?.timezone || 'Asia/Ho_Chi_Minh'}</strong>. Bấm vào
            buổi học để xem chi tiết hoặc vào lớp.
          </p>
        </div>

        {/* Lọc buổi chưa chốt (cho GV và Admin) */}
        {!isStudent && (
          <div className="flex items-center gap-2 bg-slate-50 border p-2 rounded-xl text-xs">
            <Filter className="w-4 h-4 text-slate-500" />
            <label className="flex items-center gap-1.5 cursor-pointer font-medium text-slate-700">
              <input
                type="checkbox"
                checked={outcomePendingOnly}
                onChange={(e) => setOutcomePendingOnly(e.target.checked)}
                className="rounded text-indigo-600 focus:ring-indigo-500"
              />
              Chỉ hiện buổi chưa chốt kết quả
            </label>
          </div>
        )}
      </div>

      {/* Chú giải trạng thái */}
      <div className="flex flex-wrap items-center gap-2 p-3 bg-white border rounded-xl text-xs text-slate-600">
        <span className="font-semibold text-slate-700">Trạng thái:</span>
        <span className="flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-full bg-blue-500"></span> Đã đặt (Sắp diễn ra)
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span> Hoàn thành
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-full bg-red-500"></span> Vắng mặt / Hủy muộn
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2.5 h-2.5 rounded-full bg-slate-400"></span> Đã hủy đúng hạn
        </span>
      </div>

      {/* Lịch FullCalendar */}
      <Card className="overflow-hidden">
        <CardContent className="p-4 sm:p-6">
          {isLoading ? (
            <div className="p-12 text-center text-sm text-slate-400">Đang tải lịch học…</div>
          ) : (
            <div className="calendar-container">
              <FullCalendar
                plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
                initialView="timeGridWeek"
                headerToolbar={{
                  left: 'prev,next today',
                  center: 'title',
                  right: 'timeGridWeek,dayGridMonth,timeGridDay',
                }}
                buttonText={{
                  today: 'Hôm nay',
                  month: 'Tháng',
                  week: 'Tuần',
                  day: 'Ngày',
                }}
                locale="vi"
                firstDay={1}
                slotMinTime="06:00:00"
                slotMaxTime="23:00:00"
                allDaySlot={false}
                events={calendarEvents}
                eventClick={handleEventClick}
                height="auto"
                eventTimeFormat={{
                  hour: '2-digit',
                  minute: '2-digit',
                  hour12: false,
                }}
              />
            </div>
          )}
        </CardContent>
      </Card>

      {/* Drawer chi tiết buổi học */}
      <SessionDetailDrawer
        sessionId={selectedSessionId}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
      />
    </div>
  )
}
