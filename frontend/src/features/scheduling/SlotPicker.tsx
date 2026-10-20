import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchSlots, type Slot } from '@/lib/api/scheduling'
import { Clock, Calendar, AlertCircle } from 'lucide-react'

interface SlotPickerProps {
  enrollmentId: string
  selectedSlot: Slot | null
  onSelectSlot: (slot: Slot) => void
}

export default function SlotPicker({ enrollmentId, selectedSlot, onSelectSlot }: SlotPickerProps) {
  // 14 ngày tới
  const [selectedDate, setSelectedDate] = useState<string>(() => {
    const d = new Date()
    return d.toISOString().slice(0, 10)
  })

  const dates = Array.from({ length: 14 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() + i)
    return {
      dateString: d.toISOString().slice(0, 10),
      dayOfWeek: d.toLocaleDateString('vi-VN', { weekday: 'short' }),
      dayAndMonth: d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }),
      isToday: i === 0,
    }
  })

  // Tính from và to cho ngày đã chọn (theo UTC ngày)
  const from = `${selectedDate}T00:00:00Z`
  const to = `${selectedDate}T23:59:59Z`

  const {
    data: slots = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ['available-slots', enrollmentId, selectedDate],
    queryFn: () => fetchSlots(enrollmentId, from, to),
    enabled: !!enrollmentId && !!selectedDate,
  })

  const formatSlotTime = (isoString: string) => {
    const d = new Date(isoString)
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  }

  return (
    <div className="space-y-6">
      {/* 1. Chọn ngày (Slider/Grid ngang) */}
      <div>
        <label className="block text-sm font-semibold text-slate-800 mb-2 flex items-center gap-1.5">
          <Calendar className="w-4 h-4 text-indigo-600" />
          Bước 1: Chọn ngày học (14 ngày tới)
        </label>
        <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-thin">
          {dates.map((item) => {
            const isSelected = selectedDate === item.dateString
            return (
              <button
                key={item.dateString}
                type="button"
                onClick={() => {
                  setSelectedDate(item.dateString)
                }}
                className={`flex flex-col items-center justify-center min-w-[72px] py-2.5 px-2 rounded-xl border transition-all text-xs ${
                  isSelected
                    ? 'bg-indigo-600 text-white border-indigo-600 shadow-md font-semibold'
                    : 'bg-white text-slate-700 border-slate-200 hover:border-indigo-300 hover:bg-indigo-50/50'
                }`}
              >
                <span className="uppercase text-[10px] tracking-wider opacity-80">
                  {item.isToday ? 'Hôm nay' : item.dayOfWeek}
                </span>
                <span className="text-sm font-bold mt-0.5">{item.dayAndMonth}</span>
              </button>
            )
          })}
        </div>
      </div>

      {/* 2. Danh sách khung giờ trống */}
      <div>
        <label className="block text-sm font-semibold text-slate-800 mb-2 flex items-center gap-1.5">
          <Clock className="w-4 h-4 text-indigo-600" />
          Bước 2: Chọn khung giờ rảnh phù hợp
        </label>

        {isLoading ? (
          <div className="p-8 text-center text-sm text-slate-400 bg-slate-50 rounded-xl border">
            Đang tìm các khung giờ trống của giáo viên…
          </div>
        ) : error ? (
          <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
            Không thể tải khung giờ. Vui lòng thử lại sau.
          </div>
        ) : slots.length === 0 ? (
          <div className="p-8 text-center bg-slate-50 border border-dashed rounded-xl">
            <p className="text-sm font-medium text-slate-600">
              Không có khung giờ rảnh nào trong ngày này.
            </p>
            <p className="text-xs text-slate-400 mt-1">
              Giáo viên có thể đã kín lịch hoặc đã đăng ký ngày nghỉ. Vui lòng chọn một ngày khác!
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2.5">
            {slots.map((slot) => {
              const isSelected = selectedSlot?.startsAt === slot.startsAt
              return (
                <button
                  key={slot.startsAt}
                  type="button"
                  onClick={() => onSelectSlot(slot)}
                  className={`p-3 rounded-xl border text-left transition-all relative ${
                    isSelected
                      ? 'bg-indigo-50 border-indigo-600 text-indigo-900 shadow-sm ring-2 ring-indigo-600/20'
                      : 'bg-white border-slate-200 text-slate-800 hover:border-slate-300 hover:bg-slate-50'
                  }`}
                >
                  <div className="font-semibold text-sm">
                    {formatSlotTime(slot.startsAt)} - {formatSlotTime(slot.endsAt)}
                  </div>
                  <div className="text-[11px] text-slate-500 mt-0.5 flex items-center justify-between">
                    <span>{slot.durationMinutes} phút</span>
                    {isSelected && (
                      <span className="text-[10px] font-bold text-indigo-600 bg-indigo-100 px-1.5 py-0.2 rounded">
                        Đã chọn
                      </span>
                    )}
                  </div>
                </button>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
