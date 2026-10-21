import { http, HttpResponse } from 'msw'
import type {
  CourseItem,
  PackageItem,
  StudentSummary,
  StudentProfile,
  TeacherProfile,
  EnrollmentDetail,
  TeacherAssignmentHistory,
} from '@/lib/api/catalog'
import type { StatementDto, PaymentItem } from '@/lib/api/billing'

let mockCourses: CourseItem[] = [
  {
    id: 'course-ielts',
    name: 'IELTS Intensive 1 kèm 1',
    description: 'Lộ trình luyện thi cấp tốc 4 kỹ năng chuẩn đề thi thực tế.',
    status: 'ACTIVE',
  },
  {
    id: 'course-comm',
    name: 'Tiếng Anh giao tiếp phản xạ',
    description: 'Thực hành phát âm chuẩn bản xứ, đàm thoại tự tin theo chủ đề thực tiễn.',
    status: 'ACTIVE',
  },
]

let mockPackages: PackageItem[] = [
  {
    id: 'pkg-1',
    courseId: 'course-ielts',
    name: 'Gói 10 buổi (60 phút)',
    sessionMinutes: 60,
    sessionCount: 10,
    priceVnd: 4500000,
    validityDays: 60,
    status: 'ACTIVE',
  },
  {
    id: 'pkg-2',
    courseId: 'course-ielts',
    name: 'Gói 20 buổi (60 phút)',
    sessionMinutes: 60,
    sessionCount: 20,
    priceVnd: 8500000,
    validityDays: 120,
    status: 'ACTIVE',
  },
  {
    id: 'pkg-3',
    courseId: 'course-comm',
    name: 'Gói 15 buổi (45 phút)',
    sessionMinutes: 45,
    sessionCount: 15,
    priceVnd: 3600000,
    validityDays: 90,
    status: 'ACTIVE',
  },
]

let mockTeacherProfiles: Record<string, TeacherProfile> = {
  'user-teacher-1': {
    userId: 'user-teacher-1',
    bio: '8 năm kinh nghiệm luyện thi IELTS, 8.5 Overall. Tận tâm và phương pháp hiện đại.',
    specialties: ['IELTS Speaking', 'IELTS Writing', 'Giao tiếp nâng cao'],
    certificates: 'IELTS 8.5, CELTA Grade A',
  },
  'user-teacher-2': {
    userId: 'user-teacher-2',
    bio: 'Thạc sĩ Ngôn ngữ ứng dụng Đại học Melbourne. Chuyên âm ngữ và phát âm bản xứ.',
    specialties: ['Phát âm chuẩn Anh-Mỹ', 'Tiếng Anh thương mại'],
    certificates: 'TESOL, MA in Applied Linguistics',
  },
}

let mockStudentProfiles: Record<string, StudentProfile> = {
  'user-student-1': {
    userId: 'user-student-1',
    selfReportedLevel: 'B1',
    occupation: 'Sinh viên năm 3',
    notes: 'Mục tiêu đạt IELTS 6.5 trước tháng 12 để nộp hồ sơ tốt nghiệp.',
  },
}

let mockEnrollments: EnrollmentDetail[] = [
  {
    id: 'enr-1',
    studentId: 'user-student-1',
    teacherId: 'user-teacher-1',
    packageId: 'pkg-1',
    sessionsTotal: 10,
    sessionsReserved: 1,
    sessionsConsumed: 4,
    sessionsRemaining: 5,
    sessionMinutes: 60,
    priceVnd: 4500000,
    activatedAt: '2026-10-01T08:00:00Z',
    expiresAt: '2026-12-01T23:59:59Z',
    status: 'ACTIVE',
    version: 1,
  },
  {
    id: 'enr-pending',
    studentId: 'user-student-1',
    teacherId: 'user-teacher-2',
    packageId: 'pkg-3',
    sessionsTotal: 15,
    sessionsReserved: 0,
    sessionsConsumed: 0,
    sessionsRemaining: 15,
    sessionMinutes: 45,
    priceVnd: 3600000,
    activatedAt: null,
    expiresAt: null,
    status: 'PENDING_PAYMENT',
    version: 1,
  },
]

let mockTeacherHistory: Record<string, TeacherAssignmentHistory[]> = {
  'enr-1': [
    {
      fromTeacherId: null,
      toTeacherId: 'user-teacher-1',
      changedBy: 'user-admin-1',
      reason: 'Phân công giáo viên ban đầu theo đăng ký của học viên',
      changedAt: '2026-10-01T08:00:00Z',
    },
  ],
}

let mockStatements: Record<string, StatementDto> = {
  'enr-1': {
    enrollmentId: 'enr-1',
    amountDueVnd: 4500000,
    amountPaidVnd: 4500000,
    debtVnd: 0,
    payments: [
      {
        id: 'pay-1',
        amountVnd: 4500000,
        paidAt: '2026-10-01',
        method: 'BANK_TRANSFER',
        adjustment: false,
        note: 'Chuyển khoản VCB - Kích hoạt gói',
        recordedBy: 'user-admin-1',
        recordedAt: '2026-10-01T08:05:00Z',
      },
    ],
  },
  'enr-pending': {
    enrollmentId: 'enr-pending',
    amountDueVnd: 3600000,
    amountPaidVnd: 0,
    debtVnd: 3600000,
    payments: [],
  },
}

export const catalogHandlers = [
  // Courses
  http.get('*/api/v1/courses', () => {
    return HttpResponse.json(mockCourses)
  }),

  http.post('*/api/v1/courses', async ({ request }) => {
    const body = (await request.json()) as any
    const newCourse: CourseItem = {
      id: `course-${Date.now()}`,
      name: body.name,
      description: body.description,
      status: 'ACTIVE',
    }
    mockCourses.push(newCourse)
    return HttpResponse.json(newCourse, { status: 201 })
  }),

  http.put('*/api/v1/courses/:id', async ({ params, request }) => {
    const body = (await request.json()) as any
    const c = mockCourses.find((item) => item.id === params.id)
    if (!c) return new HttpResponse(null, { status: 404 })
    c.name = body.name ?? c.name
    c.description = body.description ?? c.description
    c.status = body.status ?? c.status
    return HttpResponse.json(c)
  }),

  // Packages
  http.get('*/api/v1/packages', ({ request }) => {
    const url = new URL(request.url)
    const courseId = url.searchParams.get('courseId')
    let result = [...mockPackages]
    if (courseId) result = result.filter((p) => p.courseId === courseId)
    return HttpResponse.json(result)
  }),

  http.post('*/api/v1/packages', async ({ request }) => {
    const body = (await request.json()) as any
    const newPkg: PackageItem = {
      id: `pkg-${Date.now()}`,
      courseId: body.courseId,
      name: body.name,
      sessionMinutes: body.sessionMinutes,
      sessionCount: body.sessionCount,
      priceVnd: body.priceVnd,
      validityDays: body.validityDays,
      status: 'ACTIVE',
    }
    mockPackages.push(newPkg)
    return HttpResponse.json(newPkg, { status: 201 })
  }),

  http.put('*/api/v1/packages/:id', async ({ params, request }) => {
    const body = (await request.json()) as any
    const p = mockPackages.find((item) => item.id === params.id)
    if (!p) return new HttpResponse(null, { status: 404 })
    p.name = body.name ?? p.name
    p.sessionMinutes = body.sessionMinutes ?? p.sessionMinutes
    p.sessionCount = body.sessionCount ?? p.sessionCount
    p.priceVnd = body.priceVnd ?? p.priceVnd
    p.validityDays = body.validityDays ?? p.validityDays
    p.status = body.status ?? p.status
    return HttpResponse.json(p)
  }),

  // Profiles
  http.get('*/api/v1/teachers/:id/profile', ({ params }) => {
    const p = mockTeacherProfiles[params.id as string] || {
      userId: params.id,
      specialties: [],
    }
    return HttpResponse.json(p)
  }),

  http.put('*/api/v1/teachers/:id/profile', async ({ params, request }) => {
    const body = (await request.json()) as any
    const p: TeacherProfile = {
      userId: params.id as string,
      bio: body.bio,
      specialties: body.specialties || [],
      certificates: body.certificates,
    }
    mockTeacherProfiles[params.id as string] = p
    return HttpResponse.json(p)
  }),

  http.get('*/api/v1/students/:id/profile', ({ params }) => {
    const p = mockStudentProfiles[params.id as string] || {
      userId: params.id,
      selfReportedLevel: null,
      occupation: null,
      notes: null,
    }
    return HttpResponse.json(p)
  }),

  http.put('*/api/v1/students/:id/profile', async ({ params, request }) => {
    const body = (await request.json()) as any
    const p: StudentProfile = {
      userId: params.id as string,
      selfReportedLevel: body.selfReportedLevel,
      occupation: body.occupation,
      notes: body.notes,
    }
    mockStudentProfiles[params.id as string] = p
    return HttpResponse.json(p)
  }),

  http.get('*/api/v1/students', ({ request }) => {
    const url = new URL(request.url)
    const q = url.searchParams.get('q')?.toLowerCase()
    const needsConsulting = url.searchParams.get('needsConsulting')

    let students: StudentSummary[] = [
      {
        id: 'user-student-1',
        fullName: 'Lê Học Viên',
        email: 'student1@platform.com',
        status: 'ACTIVE',
        selfReportedLevel: 'B1',
        teacherIds: ['user-teacher-1'],
        needsConsulting: false,
      },
      {
        id: 'user-student-under16',
        fullName: 'Hoàng Bé Nhỏ',
        email: 'kid@platform.com',
        status: 'AWAITING_GUARDIAN_CONSENT',
        selfReportedLevel: null,
        teacherIds: [],
        needsConsulting: true,
      },
    ]

    if (q) {
      students = students.filter(
        (s) => s.fullName.toLowerCase().includes(q) || s.email.toLowerCase().includes(q),
      )
    }
    if (needsConsulting === 'true') {
      students = students.filter((s) => s.needsConsulting)
    }

    return HttpResponse.json(students)
  }),

  // Enrollments
  http.get('*/api/v1/enrollments', ({ request }) => {
    const url = new URL(request.url)
    const studentId = url.searchParams.get('studentId')
    const teacherId = url.searchParams.get('teacherId')
    const status = url.searchParams.get('status')

    let result = [...mockEnrollments]
    if (studentId) result = result.filter((e) => e.studentId === studentId)
    if (teacherId) result = result.filter((e) => e.teacherId === teacherId)
    if (status) result = result.filter((e) => e.status === status)

    return HttpResponse.json(result)
  }),

  http.get('*/api/v1/enrollments/:id', ({ params }) => {
    const e = mockEnrollments.find((item) => item.id === params.id)
    if (!e) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(e)
  }),

  http.post('*/api/v1/enrollments', async ({ request }) => {
    const body = (await request.json()) as any
    const pkg = mockPackages.find((p) => p.id === body.packageId)
    const newEnr: EnrollmentDetail = {
      id: `enr-${Date.now()}`,
      studentId: body.studentId,
      teacherId: body.teacherId,
      packageId: body.packageId,
      sessionsTotal: pkg ? pkg.sessionCount : 10,
      sessionsReserved: 0,
      sessionsConsumed: 0,
      sessionsRemaining: pkg ? pkg.sessionCount : 10,
      sessionMinutes: pkg ? pkg.sessionMinutes : 60,
      priceVnd: body.priceVnd ?? pkg?.priceVnd ?? 4000000,
      activatedAt: null,
      expiresAt: null,
      status: 'PENDING_PAYMENT',
      version: 1,
    }

    mockEnrollments.push(newEnr)
    mockTeacherHistory[newEnr.id] = [
      {
        fromTeacherId: null,
        toTeacherId: body.teacherId,
        changedBy: 'user-admin-1',
        reason: 'Phân công ban đầu khi tạo ghi danh',
        changedAt: new Date().toISOString(),
      },
    ]

    mockStatements[newEnr.id] = {
      enrollmentId: newEnr.id,
      amountDueVnd: newEnr.priceVnd || 0,
      amountPaidVnd: 0,
      debtVnd: newEnr.priceVnd || 0,
      payments: [],
    }

    return HttpResponse.json(newEnr, { status: 201 })
  }),

  http.put('*/api/v1/enrollments/:id/teacher', async ({ params, request }) => {
    const body = (await request.json()) as any
    const e = mockEnrollments.find((item) => item.id === params.id)
    if (!e) return new HttpResponse(null, { status: 404 })

    const history = mockTeacherHistory[e.id] || []
    history.push({
      fromTeacherId: e.teacherId,
      toTeacherId: body.teacherId,
      changedBy: 'user-admin-1',
      reason: body.reason,
      changedAt: new Date().toISOString(),
    })
    mockTeacherHistory[e.id] = history

    e.teacherId = body.teacherId
    return HttpResponse.json(e)
  }),

  http.get('*/api/v1/enrollments/:id/teacher-history', ({ params }) => {
    const list = mockTeacherHistory[params.id as string] || []
    return HttpResponse.json(list)
  }),

  http.post('*/api/v1/enrollments/:id/extend', async ({ params, request }) => {
    const body = (await request.json()) as any
    const e = mockEnrollments.find((item) => item.id === params.id)
    if (!e) return new HttpResponse(null, { status: 404 })

    e.expiresAt = body.newExpiresAt
    if (e.status === 'EXPIRED') {
      e.status = 'ACTIVE'
    }
    return HttpResponse.json(e)
  }),

  http.post('*/api/v1/enrollments/:id/cancel', async ({ params }) => {
    const e = mockEnrollments.find((item) => item.id === params.id)
    if (!e) return new HttpResponse(null, { status: 404 })
    e.status = 'CANCELLED'
    return new HttpResponse(null, { status: 204 })
  }),

  // Billing statement & manual payments
  http.get('*/api/v1/enrollments/:id/payments', ({ params }) => {
    const s = mockStatements[params.id as string] || {
      enrollmentId: params.id,
      amountDueVnd: 4500000,
      amountPaidVnd: 0,
      debtVnd: 4500000,
      payments: [],
    }
    return HttpResponse.json(s)
  }),

  http.post('*/api/v1/enrollments/:id/payments', async ({ params, request }) => {
    const body = (await request.json()) as any
    const statement = mockStatements[params.id as string]
    const enrollment = mockEnrollments.find((item) => item.id === params.id)

    const payment: PaymentItem = {
      id: `pay-${Date.now()}`,
      amountVnd: body.amountVnd,
      paidAt: body.paidAt,
      method: body.method,
      adjustment: false,
      note: body.note,
      recordedBy: 'user-admin-1',
      recordedAt: new Date().toISOString(),
    }

    if (statement) {
      statement.payments.push(payment)
      statement.amountPaidVnd += body.amountVnd
      statement.debtVnd = Math.max(0, statement.amountDueVnd - statement.amountPaidVnd)
    }

    // Nếu thanh toán lần đầu cho enrollment PENDING_PAYMENT -> kích hoạt gói
    if (enrollment && enrollment.status === 'PENDING_PAYMENT') {
      enrollment.status = 'ACTIVE'
      enrollment.activatedAt = new Date().toISOString()
      const exp = new Date()
      exp.setDate(exp.getDate() + 60)
      enrollment.expiresAt = exp.toISOString()
    }

    return HttpResponse.json(payment, { status: 201 })
  }),
]
