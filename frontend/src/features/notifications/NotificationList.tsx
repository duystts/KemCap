import type { NotificationItem } from '@/lib/api/notifications'
import { CheckCheck } from 'lucide-react'

interface NotificationListProps {
  items: NotificationItem[]
  onSelect: (item: NotificationItem) => void
  onMarkAllRead: () => void
  isLoading?: boolean
}

function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffSec = Math.floor((now.getTime() - date.getTime()) / 1000)
  if (diffSec < 60) return 'Vừa xong'
  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin} phút trước`
  const diffHours = Math.floor(diffMin / 60)
  if (diffHours < 24) return `${diffHours} giờ trước`
  const diffDays = Math.floor(diffHours / 24)
  if (diffDays < 7) return `${diffDays} ngày trước`
  return date.toLocaleDateString('vi-VN')
}

export function NotificationList({
  items,
  onSelect,
  onMarkAllRead,
  isLoading,
}: NotificationListProps) {
  return (
    <div className="flex flex-col w-80 sm:w-96 max-h-[480px]">
      <div className="flex items-center justify-between border-b px-4 py-3">
        <span className="font-semibold text-sm">Thông báo</span>
        {items.length > 0 && (
          <button
            type="button"
            onClick={onMarkAllRead}
            className="flex items-center gap-1 text-xs text-primary hover:underline"
          >
            <CheckCheck className="h-3.5 w-3.5" />
            Đọc tất cả
          </button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto divide-y divide-border">
        {isLoading && (
          <div className="p-4 text-center text-xs text-muted-foreground">Đang tải...</div>
        )}

        {!isLoading && items.length === 0 && (
          <div className="p-8 text-center text-xs text-muted-foreground">
            Chưa có thông báo nào.
          </div>
        )}

        {!isLoading &&
          items.map((item) => {
            const isUnread = !item.readAt
            return (
              <div
                key={item.id}
                role="button"
                tabIndex={0}
                onClick={() => onSelect(item)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') onSelect(item)
                }}
                className={`flex gap-3 px-4 py-3 text-left transition-colors hover:bg-muted/60 cursor-pointer ${
                  isUnread ? 'bg-primary/5' : ''
                }`}
              >
                <div className="pt-1">
                  {isUnread ? (
                    <span
                      className="block h-2 w-2 rounded-full bg-blue-600"
                      aria-label="Chưa đọc"
                    />
                  ) : (
                    <span className="block h-2 w-2 rounded-full bg-transparent" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p
                    className={`text-xs ${isUnread ? 'font-semibold text-foreground' : 'text-foreground/90'}`}
                  >
                    {item.title}
                  </p>
                  {item.body && (
                    <p className="mt-0.5 text-xs text-muted-foreground line-clamp-2">{item.body}</p>
                  )}
                  <span className="mt-1 block text-[11px] text-muted-foreground/75">
                    {formatRelativeTime(item.createdAt)}
                  </span>
                </div>
              </div>
            )
          })}
      </div>
    </div>
  )
}
