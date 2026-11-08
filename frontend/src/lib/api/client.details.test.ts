import { describe, expect, it } from 'vitest'
import { ApiError } from './client'

describe('ApiError', () => {
  it('giữ trường bổ sung ngoài chuẩn, bỏ các trường chuẩn', async () => {
    const response = new Response(
      JSON.stringify({
        status: 409,
        title: 'Conflict',
        detail: 'Ngày nghỉ trùng buổi học đã đặt',
        code: 'EXCEPTION_OVERLAPS_SESSIONS',
        instance: '/api/v1/x',
        sessions: [{ id: 's-1', startsAt: '2026-11-07T11:00:00Z' }],
      }),
      { status: 409, headers: { 'Content-Type': 'application/json' } },
    )

    const error = await ApiError.from(response)

    expect(error.code).toBe('EXCEPTION_OVERLAPS_SESSIONS')
    expect(error.details).toEqual({ sessions: [{ id: 's-1', startsAt: '2026-11-07T11:00:00Z' }] })
  })

  it('thân không phải JSON thì không có trường bổ sung', async () => {
    const error = await ApiError.from(new Response('oops', { status: 500 }))

    expect(error.code).toBe('INTERNAL_ERROR')
    expect(error.details).toEqual({})
  })
})
