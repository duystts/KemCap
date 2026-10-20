import { http, HttpResponse } from 'msw'
import type { UserItem, GuardianConsentData } from '@/lib/api/users'

let mockUsers: UserItem[] = [
  {
    id: 'user-admin-1',
    email: 'admin@platform.com',
    fullName: 'Quản Trị Viên',
    role: 'ADMIN',
    status: 'ACTIVE',
    anonymized: false,
    lastLoginAt: '2026-10-20T10:00:00Z',
  },
  {
    id: 'user-teacher-1',
    email: 'teacher1@platform.com',
    fullName: 'Nguyễn Văn Thầy',
    phone: '0912345678',
    role: 'TEACHER',
    status: 'ACTIVE',
    anonymized: false,
    lastLoginAt: '2026-10-20T08:00:00Z',
  },
  {
    id: 'user-teacher-2',
    email: 'teacher2@platform.com',
    fullName: 'Trần Thị Cô',
    phone: '0987654321',
    role: 'TEACHER',
    status: 'ACTIVE',
    anonymized: false,
  },
  {
    id: 'user-student-1',
    email: 'student1@platform.com',
    fullName: 'Lê Học Viên',
    phone: '0901112233',
    role: 'STUDENT',
    status: 'ACTIVE',
    dateOfBirth: '2005-05-15',
    anonymized: false,
    lastLoginAt: '2026-10-19T14:00:00Z',
  },
  {
    id: 'user-student-under16',
    email: 'kid@platform.com',
    fullName: 'Hoàng Bé Nhỏ',
    role: 'STUDENT',
    status: 'AWAITING_GUARDIAN_CONSENT',
    dateOfBirth: '2012-08-20',
    anonymized: false,
  },
]

let guardianConsents: Record<string, GuardianConsentData> = {}

export const usersHandlers = [
  http.get('*/api/v1/users', ({ request }) => {
    const url = new URL(request.url)
    const role = url.searchParams.get('role')
    const status = url.searchParams.get('status')
    const q = url.searchParams.get('q')?.toLowerCase()

    let result = [...mockUsers]
    if (role) result = result.filter((u) => u.role === role)
    if (status) result = result.filter((u) => u.status === status)
    if (q) {
      result = result.filter(
        (u) => u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q),
      )
    }

    return HttpResponse.json(result)
  }),

  http.get('*/api/v1/users/:id', ({ params }) => {
    const user = mockUsers.find((u) => u.id === params.id)
    if (!user) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(user)
  }),

  http.post('*/api/v1/users', async ({ request }) => {
    const body = (await request.json()) as any
    // Kiểm tra tuổi < 16
    let initialStatus = 'INVITED'
    if (body.role === 'STUDENT' && body.dateOfBirth) {
      const birthDate = new Date(body.dateOfBirth)
      const ageDiffMs = Date.now() - birthDate.getTime()
      const age = new Date(ageDiffMs).getUTCFullYear() - 1970
      if (age < 16) {
        initialStatus = 'AWAITING_GUARDIAN_CONSENT'
      }
    }

    const newUser: UserItem = {
      id: `user-${Date.now()}`,
      email: body.email,
      fullName: body.fullName,
      phone: body.phone,
      role: body.role,
      status: initialStatus as any,
      dateOfBirth: body.dateOfBirth,
      anonymized: false,
    }

    mockUsers.push(newUser)
    return HttpResponse.json(newUser, { status: 201 })
  }),

  http.patch('*/api/v1/users/:id/status', async ({ params, request }) => {
    const body = (await request.json()) as any
    const user = mockUsers.find((u) => u.id === params.id)
    if (!user) return new HttpResponse(null, { status: 404 })

    // Mô phỏng TEACHER_HAS_STUDENTS
    if (user.role === 'TEACHER' && body.status === 'INACTIVE' && user.id === 'user-teacher-1') {
      return HttpResponse.json(
        {
          code: 'TEACHER_HAS_STUDENTS',
          message: 'Giáo viên còn học viên được phân công — hãy đổi giáo viên trước.',
        },
        { status: 409 },
      )
    }

    user.status = body.status
    return HttpResponse.json(user)
  }),

  http.post('*/api/v1/users/:id/resend-invite', () => {
    return new HttpResponse(null, { status: 204 })
  }),

  http.get('*/api/v1/students/:id/guardian-consent', ({ params }) => {
    const consent = guardianConsents[params.id as string]
    if (!consent) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(consent)
  }),

  http.put('*/api/v1/students/:id/guardian-consent', async ({ params, request }) => {
    const body = (await request.json()) as GuardianConsentData
    guardianConsents[params.id as string] = body

    const user = mockUsers.find((u) => u.id === params.id)
    if (user && user.status === 'AWAITING_GUARDIAN_CONSENT') {
      user.status = 'INVITED'
    }

    return HttpResponse.json(body)
  }),
]
