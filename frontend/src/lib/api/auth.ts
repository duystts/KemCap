import { api, setAccessToken } from './client'
import type { Me, TokenResponse } from './types'

export const authApi = {
  async login(email: string, password: string) {
    const data = await api<TokenResponse>('/auth/login', {
      method: 'POST',
      body: { email, password },
      auth: false,
    })
    setAccessToken(data.accessToken)
    return data
  },
  async logout() {
    try {
      await api<void>('/auth/logout', { method: 'POST', auth: false })
    } finally {
      setAccessToken(null)
    }
  },
  acceptInvite(token: string, password: string, acceptedPolicyVersion: string) {
    return api<void>('/auth/accept-invite', {
      method: 'POST',
      body: { token, password, acceptedPolicyVersion },
      auth: false,
    })
  },
  forgotPassword(email: string) {
    return api<void>('/auth/forgot-password', { method: 'POST', body: { email }, auth: false })
  },
  resetPassword(token: string, newPassword: string) {
    return api<void>('/auth/reset-password', {
      method: 'POST',
      body: { token, newPassword },
      auth: false,
    })
  },
  me() {
    return api<Me>('/me')
  },
  acceptPolicy(policyVersion: string) {
    return api<void>('/me/consents', { method: 'POST', body: { policyVersion } })
  },
  /** Public: phiên bản chính sách hiện hành (dùng ở trang chấp nhận lời mời). */
  policy() {
    return api<{ currentPolicyVersion: string }>('/auth/policy', { auth: false })
  },
}
