import { api } from './client'

export interface NotificationItem {
  id: string
  type: string
  title: string
  body?: string
  link?: string
  readAt?: string | null
  createdAt: string
}

export interface NotificationPageResponse {
  items: NotificationItem[]
  totalElements: number
  totalPages: number
  page: number
  size: number
  unreadCount: number
}

export async function fetchNotifications(unreadOnly = false, page = 0, size = 20) {
  const query = new URLSearchParams()
  if (unreadOnly) query.set('unread', 'true')
  query.set('page', String(page))
  query.set('size', String(size))
  return api<NotificationPageResponse>(`/notifications?${query.toString()}`)
}

export async function markNotificationRead(id: string) {
  return api<void>(`/notifications/${id}/read`, { method: 'POST' })
}

export async function markAllNotificationsRead() {
  return api<void>(`/notifications/read-all`, { method: 'POST' })
}
