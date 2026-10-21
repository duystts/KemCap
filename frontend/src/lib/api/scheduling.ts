import { api } from './client'

export type DayOfWeek =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface AvailabilityRule {
  dayOfWeek: DayOfWeek
  startTime: string // "HH:mm" or "HH:mm:ss"
  endTime: string // "HH:mm" or "HH:mm:ss"
}

export interface AvailabilityException {
  id?: string
  date: string // "YYYY-MM-DD"
  startTime?: string | null
  endTime?: string | null
  reason?: string | null
}

export interface ExceptionRequest {
  date: string
  startTime?: string | null
  endTime?: string | null
  reason?: string
}

export interface Slot {
  startsAt: string // ISO Instant string
  endsAt: string // ISO Instant string
  durationMinutes: number
}

export type SessionStatus =
  | 'BOOKED'
  | 'COMPLETED'
  | 'STUDENT_NO_SHOW'
  | 'TEACHER_ABSENT'
  | 'CANCELLED_ON_TIME'
  | 'CANCELLED_LATE'
  | 'CANCELLED_SYSTEM'
  | 'INTERRUPTED'

export interface SessionItem {
  id: string
  enrollmentId: string
  teacherId: string
  studentId: string
  startsAt: string
  endsAt: string
  status: SessionStatus
  bookedBy?: string
  cancelledBy?: string
  cancelReason?: string
  rescheduledFromId?: string
  outcomeBy?: string
  teacherJoinedAt?: string
  studentJoinedAt?: string
  version: number
  // Client enrichment or display
  teacherName?: string
  studentName?: string
}

export interface BookSessionRequest {
  enrollmentId: string
  startsAt: string
}

export interface CancelPreview {
  willConsumeSession: boolean
  hoursUntilStart: number
  cutoffHours: number
  policyMessage?: string
}

export interface JoinInfo {
  roomName: string
  token: string
  serverUrl: string
}

export interface EnrollmentItem {
  id: string
  studentId: string
  teacherId: string
  packageId: string
  sessionsTotal: number
  sessionsReserved: number
  sessionsConsumed: number
  sessionsRemaining: number
  sessionMinutes: number
  priceVnd?: number | null
  activatedAt?: string
  expiresAt?: string
  status: 'PENDING_PAYMENT' | 'ACTIVE' | 'EXHAUSTED' | 'EXPIRED' | 'CANCELLED'
  version: number
}

// ----------------- API Methods -----------------

// Availability
export async function fetchTeacherAvailability(teacherId: string) {
  return api<AvailabilityRule[]>(`/teachers/${teacherId}/availability`)
}

export async function saveTeacherAvailability(teacherId: string, rules: AvailabilityRule[]) {
  return api<AvailabilityRule[]>(`/teachers/${teacherId}/availability`, {
    method: 'PUT',
    body: rules,
  })
}

export async function fetchTeacherExceptions(teacherId: string) {
  return api<AvailabilityException[]>(`/teachers/${teacherId}/availability/exceptions`)
}

export async function addTeacherException(teacherId: string, req: ExceptionRequest) {
  return api<AvailabilityException>(`/teachers/${teacherId}/availability/exceptions`, {
    method: 'POST',
    body: req,
  })
}

export async function deleteTeacherException(teacherId: string, exceptionId: string) {
  return api<void>(`/teachers/${teacherId}/availability/exceptions/${exceptionId}`, {
    method: 'DELETE',
  })
}

// Enrollments helper
export async function fetchEnrollments(params?: {
  studentId?: string
  teacherId?: string
  status?: string
}) {
  const query = new URLSearchParams()
  if (params?.studentId) query.set('studentId', params.studentId)
  if (params?.teacherId) query.set('teacherId', params.teacherId)
  if (params?.status) query.set('status', params.status)
  const qs = query.toString()
  return api<EnrollmentItem[]>(`/enrollments${qs ? `?${qs}` : ''}`)
}

// Slots
export async function fetchSlots(enrollmentId: string, from?: string, to?: string) {
  const query = new URLSearchParams()
  if (from) query.set('from', from)
  if (to) query.set('to', to)
  const qs = query.toString()
  return api<Slot[]>(`/enrollments/${enrollmentId}/slots${qs ? `?${qs}` : ''}`)
}

// Sessions
export async function bookSession(req: BookSessionRequest) {
  return api<SessionItem>('/sessions', {
    method: 'POST',
    body: req,
  })
}

export async function fetchSessions(params?: {
  from?: string
  to?: string
  teacherId?: string
  studentId?: string
  status?: SessionStatus
  outcomePending?: boolean
}) {
  const query = new URLSearchParams()
  if (params?.from) query.set('from', params.from)
  if (params?.to) query.set('to', params.to)
  if (params?.teacherId) query.set('teacherId', params.teacherId)
  if (params?.studentId) query.set('studentId', params.studentId)
  if (params?.status) query.set('status', params.status)
  if (params?.outcomePending !== undefined) {
    query.set('outcomePending', String(params.outcomePending))
  }
  const qs = query.toString()
  return api<SessionItem[]>(`/sessions${qs ? `?${qs}` : ''}`)
}

export async function fetchSession(id: string) {
  return api<SessionItem>(`/sessions/${id}`)
}

export async function previewCancelSession(id: string) {
  return api<CancelPreview>(`/sessions/${id}/cancel-preview`)
}

export async function cancelSession(id: string, reason?: string) {
  return api<SessionItem>(`/sessions/${id}/cancel`, {
    method: 'POST',
    body: { reason: reason || 'Học viên yêu cầu hủy' },
  })
}

export async function rescheduleSession(id: string, newStartsAt: string) {
  return api<SessionItem>(`/sessions/${id}/reschedule`, {
    method: 'POST',
    body: { newStartsAt },
  })
}

export async function recordOutcome(id: string, outcome: SessionStatus, note?: string) {
  return api<SessionItem>(`/sessions/${id}/outcome`, {
    method: 'POST',
    body: { outcome, note: note || '' },
  })
}

export async function joinClassroom(id: string) {
  return api<JoinInfo>(`/sessions/${id}/join`, {
    method: 'POST',
  })
}
