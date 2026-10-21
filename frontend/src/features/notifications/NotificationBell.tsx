import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationItem,
} from '@/lib/api/notifications'
import { NotificationList } from './NotificationList'

export function NotificationBell() {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  // Polling mỗi 60 giây theo đặc tả T136
  const { data, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => fetchNotifications(false, 0, 20),
    refetchInterval: 60_000,
  })

  const markOneMutation = useMutation({
    mutationFn: (id: string) => markNotificationRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const markAllMutation = useMutation({
    mutationFn: () => markAllNotificationsRead(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  // Đóng popover khi click ra ngoài
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    if (open) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [open])

  const handleSelectItem = (item: NotificationItem) => {
    if (!item.readAt) {
      markOneMutation.mutate(item.id)
    }
    setOpen(false)
    if (item.link) {
      // Nếu link bắt đầu bằng / thì navigate nội bộ, ngược lại xử lý
      if (item.link.startsWith('http')) {
        try {
          const url = new URL(item.link)
          navigate(url.pathname + url.search)
        } catch {
          window.location.href = item.link
        }
      } else {
        navigate(item.link)
      }
    }
  }

  const unreadCount = data?.unreadCount ?? 0

  return (
    <div className="relative inline-block" ref={containerRef}>
      <Button
        variant="ghost"
        size="sm"
        aria-label="Thông báo"
        className="relative h-9 w-9 p-0"
        onClick={() => setOpen((prev) => !prev)}
      >
        <Bell className="h-4 w-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold text-white">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </Button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 rounded-lg border border-border bg-popover text-popover-foreground shadow-lg animate-in fade-in-0 zoom-in-95">
          <NotificationList
            items={data?.items ?? []}
            onSelect={handleSelectItem}
            onMarkAllRead={() => markAllMutation.mutate()}
            isLoading={isLoading}
          />
        </div>
      )}
    </div>
  )
}
