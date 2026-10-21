import { http, HttpResponse } from 'msw'
import type {
  AvailabilityRule,
  AvailabilityException,
  Slot,
  SessionItem,
  EnrollmentItem,
  CancelPreview,
  JoinInfo,
} from '@/lib/api/scheduling'

const API = '*/api/v1'

const mockRules: AvailabilityRule[] = [
  { dayOfWeek: 'MONDAY', startTime: '08:00', endTime: '12:00' },
  { dayOfWeek: 'MONDAY', startTime: '14:00', endTime: '18:00' },
  { dayOfWeek: 'WEDNESDAY', startTime: '08:00', endTime: '12:00' },
  { dayOfWeek: 'WEDNESDAY', startTime: '14:00', endTime: '18:00' },
  { dayOfWeek: 'FRIDAY', startTime: '08:00', endTime: '12:00' },
  { dayOfWeek: 'FRIDAY', startTime: '14:00', endTime: '18:00' },
]

const mockExceptions: AvailabilityException[] = [
  {
    id: 'ex-1',
    date: '2026-11-20',
    startTime: '08:00',
    endTime: '18:00',
    reason: 'Nghỉ khám sức khỏe định kỳ',
  },
]

const mockEnrollments: EnrollmentItem[] = [
  {
    id: 'enr-1',
    studentId: '00000000-0000-0000-0000-000000000003',
    teacherId: '00000000-0000-0000-0000-000000000002',
    packageId: 'pkg-1',
    sessionsTotal: 10,
    sessionsReserved: 2,
    sessionsConsumed: 3,
    sessionsRemaining: 5,
    sessionMinutes: 45,
    priceVnd: 2500000,
    activatedAt: '2026-10-01T00:00:00Z',
    expiresAt: '2026-12-31T23:59:59Z',
    status: 'ACTIVE',
    version: 1,
  },
]

let mockSessions: SessionItem[] = [
  {
    id: 'sess-1',
    enrollmentId: 'enr-1',
    teacherId: '00000000-0000-0000-0000-000000000002',
    studentId: '00000000-0000-0000-0000-000000000003',
    startsAt: new Date(Date.now() + 3600 * 1000 * 2).toISOString(),
    endsAt: new Date(Date.now() + 3600 * 1000 * 2 + 45 * 60 * 1000).toISOString(),
    status: 'BOOKED',
    version: 1,
  },
  {
    id: 'sess-2',
    enrollmentId: 'enr-1',
    teacherId: '00000000-0000-0000-0000-000000000002',
    studentId: '00000000-0000-0000-0000-000000000003',
    startsAt: new Date(Date.now() - 3600 * 1000 * 24).toISOString(),
    endsAt: new Date(Date.now() - 3600 * 1000 * 24 + 45 * 60 * 1000).toISOString(),
    status: 'COMPLETED',
    version: 1,
  },
]

export const schedulingHandlers = [
  // 1. Availability
  http.get(`${API}/teachers/:teacherId/availability`, () => {
    return HttpResponse.json(mockRules)
  }),

  http.put(`${API}/teachers/:teacherId/availability`, async ({ request }) => {
    const rules = (await request.json()) as AvailabilityRule[]
    mockRules.length = 0
    mockRules.push(...rules)
    return HttpResponse.json(mockRules)
  }),

  http.get(`${API}/teachers/:teacherId/availability/exceptions`, () => {
    return HttpResponse.json(mockExceptions)
  }),

  http.post(`${API}/teachers/:teacherId/availability/exceptions`, async ({ request }) => {
    const body = (await request.json()) as AvailabilityException
    const created: AvailabilityException = {
      id: `ex-${Date.now()}`,
      ...body,
    }
    mockExceptions.push(created)
    return HttpResponse.json(created, { status: 201 })
  }),

  http.delete(`${API}/teachers/:teacherId/availability/exceptions/:exceptionId`, ({ params }) => {
    const { exceptionId } = params
    const idx = mockExceptions.findIndex((e) => e.id === exceptionId)
    if (idx !== -1) mockExceptions.splice(idx, 1)
    return new HttpResponse(null, { status: 204 })
  }),

  // 2. Enrollments
  http.get(`${API}/enrollments`, () => {
    return HttpResponse.json(mockEnrollments)
  }),

  // 3. Slots
  http.get(`${API}/enrollments/:enrollmentId/slots`, ({ request }) => {
    const url = new URL(request.url)
    const fromParam = url.searchParams.get('from')
    const baseDate = fromParam ? new Date(fromParam) : new Date()

    // Sinh 4 slot mẫu
    const sampleSlots: Slot[] = [9, 10, 14, 15].map((hour) => {
      const start = new Date(baseDate)
      start.setHours(hour, 0, 0, 0)
      const end = new Date(start.getTime() + 45 * 60 * 1000)
      return {
        startsAt: start.toISOString(),
        endsAt: end.toISOString(),
        durationMinutes: 45,
      }
    })
    return HttpResponse.json(sampleSlots)
  }),

  // 4. Sessions
  http.get(`${API}/sessions`, () => {
    return HttpResponse.json(mockSessions)
  }),

  http.post(`${API}/sessions`, async ({ request }) => {
    const body = (await request.json()) as { enrollmentId: string; startsAt: string }
    const start = new Date(body.startsAt)
    const end = new Date(start.getTime() + 45 * 60 * 1000)

    const created: SessionItem = {
      id: `sess-${Date.now()}`,
      enrollmentId: body.enrollmentId,
      teacherId: '00000000-0000-0000-0000-000000000002',
      studentId: '00000000-0000-0000-0000-000000000003',
      startsAt: start.toISOString(),
      endsAt: end.toISOString(),
      status: 'BOOKED',
      version: 1,
    }
    mockSessions.unshift(created)
    return HttpResponse.json(created, { status: 201 })
  }),

  http.get(`${API}/sessions/:id`, ({ params }) => {
    const session = mockSessions.find((s) => s.id === params.id)
    if (!session) {
      return HttpResponse.json(
        { status: 404, code: 'SESSION_NOT_FOUND', detail: 'Không tìm thấy buổi học' },
        { status: 404 },
      )
    }
    return HttpResponse.json(session)
  }),

  http.get(`${API}/sessions/:id/cancel-preview`, ({ params }) => {
    const session = mockSessions.find((s) => s.id === params.id)
    const hours = session ? (new Date(session.startsAt).getTime() - Date.now()) / (3600 * 1000) : 24
    const willConsume = hours < 12
    const preview: CancelPreview = {
      willConsumeSession: willConsume,
      hoursUntilStart: Math.round(hours),
      cutoffHours: 12,
      policyMessage: willConsume
        ? 'Hủy muộn (dưới 12 tiếng): Buổi học này vẫn sẽ bị tính trừ vào gói của bạn.'
        : 'Hủy đúng hạn (trên 12 tiếng): Bạn sẽ được hoàn lại 1 buổi vào gói học.',
    }
    return HttpResponse.json(preview)
  }),

  http.post(`${API}/sessions/:id/cancel`, async ({ params, request }) => {
    const session = mockSessions.find((s) => s.id === params.id)
    if (!session) {
      return HttpResponse.json({ status: 404, code: 'SESSION_NOT_FOUND' }, { status: 404 })
    }
    const body = (await request.json().catch(() => ({}))) as { reason?: string }
    const hours = (new Date(session.startsAt).getTime() - Date.now()) / (3600 * 1000)
    session.status = hours < 12 ? 'CANCELLED_LATE' : 'CANCELLED_ON_TIME'
    session.cancelReason = body?.reason || 'Học viên yêu cầu hủy'
    return HttpResponse.json(session)
  }),

  http.post(`${API}/sessions/:id/reschedule`, async ({ params, request }) => {
    const session = mockSessions.find((s) => s.id === params.id)
    if (!session) {
      return HttpResponse.json({ status: 404, code: 'SESSION_NOT_FOUND' }, { status: 404 })
    }
    const body = (await request.json()) as { newStartsAt: string }
    const start = new Date(body.newStartsAt)
    const end = new Date(start.getTime() + 45 * 60 * 1000)
    session.startsAt = start.toISOString()
    session.endsAt = end.toISOString()
    return HttpResponse.json(session)
  }),

  http.post(`${API}/sessions/:id/outcome`, async ({ params, request }) => {
    const session = mockSessions.find((s) => s.id === params.id)
    if (!session) {
      return HttpResponse.json({ status: 404, code: 'SESSION_NOT_FOUND' }, { status: 404 })
    }
    const body = (await request.json()) as { outcome: SessionItem['status']; note?: string }
    session.status = body.outcome
    return HttpResponse.json(session)
  }),

  http.post(`${API}/sessions/:id/join`, ({ params }) => {
    const joinInfo: JoinInfo = {
      roomName: `session-${params.id}`,
      token: 'mock-livekit-jwt-token',
      serverUrl: 'http://localhost:7880',
    }
    return HttpResponse.json(joinInfo)
  }),
]
