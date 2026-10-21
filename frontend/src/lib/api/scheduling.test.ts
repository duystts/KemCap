import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  fetchTeacherAvailability,
  saveTeacherAvailability,
  fetchSlots,
  bookSession,
  previewCancelSession,
  cancelSession,
  joinClassroom,
} from './scheduling'
import { setAccessToken } from './client'

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('scheduling api client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    setAccessToken(null)
  })

  it('fetchTeacherAvailability calls correct endpoint', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        json(200, [{ dayOfWeek: 'MONDAY', startTime: '08:00', endTime: '12:00' }]),
      )

    const rules = await fetchTeacherAvailability('teacher-123')
    expect(rules).toHaveLength(1)
    expect(rules[0].dayOfWeek).toBe('MONDAY')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/teachers/teacher-123/availability'),
      expect.any(Object),
    )
  })

  it('saveTeacherAvailability sends PUT request with rules payload', async () => {
    const payload = [{ dayOfWeek: 'TUESDAY' as const, startTime: '09:00', endTime: '11:00' }]
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, payload))

    const result = await saveTeacherAvailability('teacher-123', payload)
    expect(result).toEqual(payload)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/teachers/teacher-123/availability'),
      expect.objectContaining({
        method: 'PUT',
        body: JSON.stringify(payload),
      }),
    )
  })

  it('fetchSlots attaches query parameters correctly', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      json(200, [
        {
          startsAt: '2026-11-01T10:00:00Z',
          endsAt: '2026-11-01T10:45:00Z',
          durationMinutes: 45,
        },
      ]),
    )

    const slots = await fetchSlots('enr-1', '2026-11-01T00:00:00Z', '2026-11-01T23:59:59Z')
    expect(slots).toHaveLength(1)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining(
        '/enrollments/enr-1/slots?from=2026-11-01T00%3A00%3A00Z&to=2026-11-01T23%3A59%3A59Z',
      ),
      expect.any(Object),
    )
  })

  it('bookSession sends POST request with enrollmentId and startsAt', async () => {
    const sessionResponse = {
      id: 'sess-1',
      enrollmentId: 'enr-1',
      startsAt: '2026-11-01T10:00:00Z',
      endsAt: '2026-11-01T10:45:00Z',
      status: 'BOOKED',
    }
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(201, sessionResponse))

    const result = await bookSession({
      enrollmentId: 'enr-1',
      startsAt: '2026-11-01T10:00:00Z',
    })
    expect(result.id).toBe('sess-1')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/sessions'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          enrollmentId: 'enr-1',
          startsAt: '2026-11-01T10:00:00Z',
        }),
      }),
    )
  })

  it('previewCancelSession returns penalty preview data', async () => {
    const previewData = {
      willConsumeSession: false,
      hoursUntilStart: 24,
      cutoffHours: 12,
      policyMessage: 'Hủy đúng hạn',
    }
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, previewData))

    const preview = await previewCancelSession('sess-1')
    expect(preview.willConsumeSession).toBe(false)
    expect(preview.hoursUntilStart).toBe(24)
  })

  it('cancelSession calls cancel endpoint with reason', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(null, { status: 204 }))

    await cancelSession('sess-1', 'Bận việc đột xuất')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/sessions/sess-1/cancel'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('Bận việc đột xuất'),
      }),
    )
  })

  it('joinClassroom requests room token from join endpoint', async () => {
    const joinInfo = {
      roomName: 'room-1',
      token: 'jwt-123',
      serverUrl: 'http://localhost:7880',
    }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, joinInfo))

    const result = await joinClassroom('sess-1')
    expect(result.token).toBe('jwt-123')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/sessions/sess-1/join'),
      expect.objectContaining({ method: 'POST' }),
    )
  })
})
