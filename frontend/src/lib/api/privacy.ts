import { api } from './client'

export type DataRequestType = 'ERASURE' | 'ACCESS'
export type DataRequestStatus = 'RECEIVED' | 'COMPLETED' | 'REJECTED'

export interface DataRequestItem {
  id: string
  studentId: string
  requesterName: string
  type: DataRequestType
  receivedOn: string // YYYY-MM-DD
  status: DataRequestStatus
  resolvedAt?: string | null
  resolutionNote?: string | null
  handledBy?: string | null
}

export interface GuardianConsentItem {
  studentId: string
  guardianName: string
  relationship: string
  guardianContact: string
  consentedOn: string // YYYY-MM-DD
}

export function fetchDataRequests(): Promise<DataRequestItem[]> {
  return api<DataRequestItem[]>('/data-subject-requests')
}

export function createDataRequest(data: {
  studentId: string
  requesterName: string
  type: DataRequestType
  receivedOn: string
}): Promise<DataRequestItem> {
  return api<DataRequestItem>('/data-subject-requests', { method: 'POST', body: data })
}

export function rejectDataRequest(id: string, resolutionNote: string): Promise<DataRequestItem> {
  return api<DataRequestItem>(`/data-subject-requests/${id}/reject`, {
    method: 'POST',
    body: { resolutionNote },
  })
}

export function completeAccessRequest(id: string): Promise<DataRequestItem> {
  return api<DataRequestItem>(`/data-subject-requests/${id}/complete-access`, { method: 'POST' })
}

export function anonymizeStudent(id: string): Promise<DataRequestItem> {
  return api<DataRequestItem>(`/data-subject-requests/${id}/anonymize`, {
    method: 'POST',
    body: { confirm: true },
  })
}

export interface SessionFeedbackRecord {
  sessionId: string
  stars: number
  comment?: string | null
}

export function fetchFeedbackRecord(sessionId: string): Promise<SessionFeedbackRecord> {
  return api<SessionFeedbackRecord>(`/sessions/${sessionId}/feedback`)
}

export interface PolicyConsentItem {
  policyVersion: string
  acceptedAt: string
}

export function fetchPolicyConsents(userId: string): Promise<PolicyConsentItem[]> {
  return api<PolicyConsentItem[]>(`/users/${userId}/policy-consents`)
}

export interface UserNotificationItem {
  id: string
  title: string
  body?: string | null
  createdAt: string
}

export function fetchUserNotifications(userId: string): Promise<UserNotificationItem[]> {
  return api<UserNotificationItem[]>(`/users/${userId}/notifications`)
}

export interface OutcomeChangeItem {
  fromStatus?: string | null
  toStatus: string
  note?: string | null
  createdAt: string
}

export function fetchOutcomeHistory(sessionId: string): Promise<OutcomeChangeItem[]> {
  return api<OutcomeChangeItem[]>(`/sessions/${sessionId}/outcome-history`)
}

export function fetchGuardianConsent(studentId: string): Promise<GuardianConsentItem> {
  return api<GuardianConsentItem>(`/students/${studentId}/guardian-consent`)
}
