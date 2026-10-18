import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, getAccessToken, setAccessToken } from './client'

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('api client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    setAccessToken(null)
  })

  it('refreshes once on TOKEN_EXPIRED and retries the request', async () => {
    setAccessToken('old')
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(401, { status: 401, code: 'TOKEN_EXPIRED' }))
      .mockResolvedValueOnce(json(200, { accessToken: 'new', user: {} }))
      .mockResolvedValueOnce(json(200, { ok: true }))

    await expect(api<{ ok: boolean }>('/me')).resolves.toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(getAccessToken()).toBe('new')
    const retryHeaders = fetchMock.mock.calls[2][1]?.headers as Record<string, string>
    expect(retryHeaders.Authorization).toBe('Bearer new')
  })

  it('maps Problem Details to ApiError with business code', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      json(409, { status: 409, code: 'SLOT_TAKEN', detail: 'trùng lịch' }),
    )
    const error = (await api('/sessions', { method: 'POST', body: {} }).catch(
      (e: unknown) => e,
    )) as ApiError
    expect(error).toBeInstanceOf(ApiError)
    expect(error.code).toBe('SLOT_TAKEN')
    expect(error.status).toBe(409)
  })

  it('emits consent-required event on CONSENT_REQUIRED', async () => {
    const listener = vi.fn()
    window.addEventListener('api:consent-required', listener)
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      json(403, { status: 403, code: 'CONSENT_REQUIRED' }),
    )
    await api('/students').catch(() => undefined)
    expect(listener).toHaveBeenCalledOnce()
    window.removeEventListener('api:consent-required', listener)
  })
})
