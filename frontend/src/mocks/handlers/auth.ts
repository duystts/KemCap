import { http, HttpResponse } from 'msw'
import type { Me, Role } from '@/lib/api/types'

const API = '*/api/v1'
const POLICY_VERSION = '2026-11'

interface MockUser {
  me: Me
  password: string
}

// Tài khoản demo giống IdentityDemoSeed (mật khẩu Demo@1234)
const users: MockUser[] = (
  [
    ['admin@demo.local', 'Giáo vụ Demo', 'ADMIN'],
    ['teacher1@demo.local', 'Nguyễn Thị Lan', 'TEACHER'],
    ['student1@demo.local', 'Lê Hoàng An', 'STUDENT'],
  ] as [string, string, Role][]
).map(([email, fullName, role], i) => ({
  password: 'Demo@1234',
  me: {
    id: `00000000-0000-0000-0000-00000000000${i + 1}`,
    email,
    fullName,
    phone: null,
    role,
    status: 'ACTIVE',
    timezone: 'Asia/Ho_Chi_Minh',
    consentRequired: false,
    currentPolicyVersion: POLICY_VERSION,
  },
}))

let current: MockUser | null = null

const problem = (status: number, code: string, detail: string) =>
  HttpResponse.json({ status, code, detail }, { status })

const tokenResponse = (u: MockUser) => ({
  accessToken: `mock-token-${u.me.id}`,
  user: {
    id: u.me.id,
    email: u.me.email,
    fullName: u.me.fullName,
    role: u.me.role,
    timezone: u.me.timezone,
  },
})

/** Mock nhóm identity theo contracts/rest-api.md (D4). */
export const authHandlers = [
  http.get(`${API}/auth/policy`, () => HttpResponse.json({ currentPolicyVersion: POLICY_VERSION })),
  http.post(`${API}/auth/login`, async ({ request }) => {
    const { email, password } = (await request.json()) as { email: string; password: string }
    const user = users.find((u) => u.me.email === email.toLowerCase() && u.password === password)
    if (!user) return problem(401, 'UNAUTHENTICATED', 'Email hoặc mật khẩu không đúng')
    current = user
    return HttpResponse.json(tokenResponse(user))
  }),
  http.post(`${API}/auth/refresh`, () =>
    current
      ? HttpResponse.json(tokenResponse(current))
      : problem(401, 'UNAUTHENTICATED', 'Chưa đăng nhập'),
  ),
  http.post(`${API}/auth/logout`, () => {
    current = null
    return new HttpResponse(null, { status: 204 })
  }),
  http.post(`${API}/auth/accept-invite`, () => new HttpResponse(null, { status: 204 })),
  http.post(`${API}/auth/forgot-password`, () => new HttpResponse(null, { status: 202 })),
  http.post(`${API}/auth/reset-password`, () => new HttpResponse(null, { status: 204 })),
  http.get(`${API}/me`, () =>
    current ? HttpResponse.json(current.me) : problem(401, 'UNAUTHENTICATED', 'Chưa đăng nhập'),
  ),
  http.post(`${API}/me/consents`, () => {
    if (current) current.me.consentRequired = false
    return new HttpResponse(null, { status: 204 })
  }),
]
