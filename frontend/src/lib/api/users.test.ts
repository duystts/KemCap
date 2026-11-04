import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  fetchUsers,
  createUser,
  updateUserStatus,
  resendInvite,
  saveGuardianConsent,
} from './users'

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('users API client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetchUsers constructs query parameters correctly', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, []))

    await fetchUsers({ role: 'STUDENT', status: 'ACTIVE', q: 'John' })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/users?role=STUDENT&status=ACTIVE&q=John'),
      expect.any(Object),
    )
  })

  it('createUser sends POST request with user details', async () => {
    const user = { id: 'u-1', email: 'test@example.com', fullName: 'Test User', role: 'STUDENT' }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(201, user))

    const res = await createUser({
      email: 'test@example.com',
      fullName: 'Test User',
      role: 'STUDENT',
      dateOfBirth: '2010-01-01',
    })

    expect(res).toEqual(user)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/users'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('test@example.com'),
      }),
    )
  })

  it('updateUserStatus sends PATCH with status', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(json(200, { id: 'u-1', status: 'INACTIVE' }))

    await updateUserStatus('u-1', 'INACTIVE')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/users/u-1/status'),
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ status: 'INACTIVE' }),
      }),
    )
  })

  it('resendInvite sends POST request', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(null, { status: 204 }))

    await resendInvite('u-1')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/users/u-1/resend-invite'),
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('saveGuardianConsent sends PUT request', async () => {
    const consent = {
      guardianName: 'Parent',
      relationship: 'Mother',
      guardianContact: '0901234567',
      consentedOn: '2026-10-20',
    }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(json(200, consent))

    await saveGuardianConsent('s-1', consent)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students/s-1/guardian-consent'),
      expect.objectContaining({
        method: 'PUT',
        body: expect.stringContaining('Parent'),
      }),
    )
  })
})
