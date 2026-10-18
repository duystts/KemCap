// Type viết tay theo contracts/rest-api.md cho tới khi `npm run gen:api` sinh schema.d.ts từ OpenAPI.

export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT'
export type UserStatus = 'AWAITING_GUARDIAN_CONSENT' | 'INVITED' | 'ACTIVE' | 'INACTIVE'

export interface UserSummary {
  id: string
  email: string
  fullName: string
  role: Role
  timezone: string
}

export interface TokenResponse {
  accessToken: string
  user: UserSummary
}

export interface Me {
  id: string
  email: string
  fullName: string
  phone: string | null
  role: Role
  status: UserStatus
  timezone: string
  consentRequired: boolean
  currentPolicyVersion: string
}

export interface ProblemDetail {
  status: number
  title?: string
  detail?: string
  code?: string
  errors?: { field: string; message: string }[]
}
