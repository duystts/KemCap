import { http, HttpResponse } from 'msw'

const API = '*/api/v1'

const mockNotifications = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    type: 'SESSION_BOOKED',
    title: 'Đặt lịch học thành công',
    body: 'Buổi học lúc 18:00 ngày 25/10/2026 với giáo viên Nguyễn Thị Lan.',
    link: '/student/schedule',
    readAt: null,
    createdAt: new Date(Date.now() - 1000 * 60 * 15).toISOString(),
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    type: 'ENROLLMENT_ACTIVATED',
    title: 'Gói học đã được kích hoạt',
    body: 'Gói học tiếng Anh giao tiếp 12 buổi của bạn đã sẵn sàng sử dụng.',
    link: '/student/packages',
    readAt: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(),
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(),
  },
]

export const notificationHandlers = [
  http.get(`${API}/notifications`, () => {
    const unreadCount = mockNotifications.filter((n) => !n.readAt).length
    return HttpResponse.json({
      items: mockNotifications,
      totalElements: mockNotifications.length,
      totalPages: 1,
      page: 0,
      size: 20,
      unreadCount,
    })
  }),

  http.post(`${API}/notifications/:id/read`, ({ params }) => {
    const target = mockNotifications.find((n) => n.id === params.id)
    if (target) {
      target.readAt = new Date().toISOString()
    }
    return new HttpResponse(null, { status: 200 })
  }),

  http.post(`${API}/notifications/read-all`, () => {
    const now = new Date().toISOString()
    mockNotifications.forEach((n) => {
      n.readAt = now
    })
    return new HttpResponse(null, { status: 200 })
  }),
]
