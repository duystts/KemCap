import { api } from './client'
import type { Role } from './types'

export type UserStatus = 'AWAITING_GUARDIAN_CONSENT' | 'INVITED' | 'ACTIVE' | 'INACTIVE'

export interface UserItem {
  id: string
  email: string
  fullName: string
  phone?: string | null
  role: Role
  status: UserStatus
  dateOfBirth?: string | null
  lastLoginAt?: string | null
  anonymized: boolean
}

export interface CreateUserData {
  email: string
  fullName: string
  role: Role
  phone?: string
  dateOfBirth?: string
}

export interface GuardianConsentData {
  guardianName: string
  relationship: string
  guardianContact: string
  consentedOn: string // YYYY-MM-DD
}

export function fetchUsers(params?: {
  role?: Role
  status?: UserStatus
  q?: string
}): Promise<UserItem[]> {
  const query = new URLSearchParams()
  if (params?.role) query.set('role', params.role)
  if (params?.status) query.set('status', params.status)
  if (params?.q) query.set('q', params.q)
  const qs = query.toString()
  return api<UserItem[]>(qs ? `/users?${qs}` : '/users')
}

export function fetchUser(id: string): Promise<UserItem> {
  return api<UserItem>(`/users/${id}`)
}

export function createUser(data: CreateUserData): Promise<UserItem> {
  return api<UserItem>('/users', {
    method: 'POST',
    body: data,
  })
}

export function updateUserStatus(id: string, status: 'ACTIVE' | 'INACTIVE'): Promise<UserItem> {
  return api<UserItem>(`/users/${id}/status`, {
    method: 'PATCH',
    body: { status },
  })
}

export function resendInvite(id: string): Promise<void> {
  return api<void>(`/users/${id}/resend-invite`, {
    method: 'POST',
  })
}

export function fetchGuardianConsent(studentId: string): Promise<GuardianConsentData | null> {
  return api<GuardianConsentData>(`/students/${studentId}/guardian-consent`)
}

export function saveGuardianConsent(
  studentId: string,
  data: GuardianConsentData,
): Promise<GuardianConsentData> {
  return api<GuardianConsentData>(`/students/${studentId}/guardian-consent`, {
    method: 'PUT',
    body: data,
  })
}
