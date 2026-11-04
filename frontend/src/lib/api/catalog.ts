import { api } from './client'

export type CatalogStatus = 'ACTIVE' | 'ARCHIVED'
export type CefrLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2'
export type EnrollmentStatus = 'PENDING_PAYMENT' | 'ACTIVE' | 'COMPLETED' | 'EXPIRED' | 'CANCELLED'

export interface CourseItem {
  id: string
  name: string
  description?: string | null
  status: CatalogStatus
}

export interface PackageItem {
  id: string
  courseId: string
  name: string
  sessionMinutes: 25 | 45 | 60 | 90
  sessionCount: number
  priceVnd: number
  validityDays: number
  status: CatalogStatus
}

export interface TeacherProfile {
  userId: string
  bio?: string | null
  specialties: string[]
  certificates?: string | null
}

export interface StudentProfile {
  userId: string
  selfReportedLevel?: CefrLevel | null
  occupation?: string | null
  notes?: string | null
}

export interface StudentSummary {
  id: string
  fullName: string
  email: string
  status: string
  selfReportedLevel?: CefrLevel | null
  teacherIds: string[]
  needsConsulting: boolean
}

export interface EnrollmentDetail {
  id: string
  studentId: string
  teacherId: string
  packageId: string
  sessionsTotal: number
  sessionsReserved: number
  sessionsConsumed: number
  sessionsRemaining: number
  sessionMinutes: number
  priceVnd?: number | null
  activatedAt?: string | null
  expiresAt?: string | null
  status: EnrollmentStatus
  version: number
}

export interface TeacherAssignmentHistory {
  fromTeacherId?: string | null
  toTeacherId: string
  changedBy: string
  reason: string
  changedAt: string
}

// Courses
export function fetchCourses(): Promise<CourseItem[]> {
  return api<CourseItem[]>('/courses')
}

export function createCourse(data: { name: string; description?: string }): Promise<CourseItem> {
  return api<CourseItem>('/courses', {
    method: 'POST',
    body: data,
  })
}

export function updateCourse(
  id: string,
  data: { name: string; description?: string; status?: CatalogStatus },
): Promise<CourseItem> {
  return api<CourseItem>(`/courses/${id}`, {
    method: 'PUT',
    body: data,
  })
}

// Packages
export function fetchPackages(courseId?: string): Promise<PackageItem[]> {
  const query = courseId ? `?courseId=${encodeURIComponent(courseId)}` : ''
  return api<PackageItem[]>(`/packages${query}`)
}

export function createPackage(data: {
  courseId: string
  name: string
  sessionMinutes: 25 | 45 | 60 | 90
  sessionCount: number
  priceVnd: number
  validityDays: number
}): Promise<PackageItem> {
  return api<PackageItem>('/packages', {
    method: 'POST',
    body: data,
  })
}

export function updatePackage(
  id: string,
  data: {
    name: string
    sessionMinutes: 25 | 45 | 60 | 90
    sessionCount: number
    priceVnd: number
    validityDays: number
    status?: CatalogStatus
  },
): Promise<PackageItem> {
  return api<PackageItem>(`/packages/${id}`, {
    method: 'PUT',
    body: data,
  })
}

// Profiles
export function fetchTeacherProfile(id: string): Promise<TeacherProfile> {
  return api<TeacherProfile>(`/teachers/${id}/profile`)
}

export function updateTeacherProfile(
  id: string,
  data: { bio?: string; specialties?: string[]; certificates?: string },
): Promise<TeacherProfile> {
  return api<TeacherProfile>(`/teachers/${id}/profile`, {
    method: 'PUT',
    body: data,
  })
}

export function fetchStudentProfile(id: string): Promise<StudentProfile> {
  return api<StudentProfile>(`/students/${id}/profile`)
}

export function updateStudentProfile(
  id: string,
  data: { selfReportedLevel?: CefrLevel; occupation?: string; notes?: string },
): Promise<StudentProfile> {
  return api<StudentProfile>(`/students/${id}/profile`, {
    method: 'PUT',
    body: data,
  })
}

export function fetchStudents(params?: {
  q?: string
  teacherId?: string
  status?: string
  needsConsulting?: boolean
}): Promise<StudentSummary[]> {
  const query = new URLSearchParams()
  if (params?.q) query.set('q', params.q)
  if (params?.teacherId) query.set('teacherId', params.teacherId)
  if (params?.status) query.set('status', params.status)
  if (params?.needsConsulting !== undefined) {
    query.set('needsConsulting', String(params.needsConsulting))
  }
  const qs = query.toString()
  return api<StudentSummary[]>(qs ? `/students?${qs}` : '/students')
}

// Enrollments
export function fetchEnrollments(params?: {
  studentId?: string
  teacherId?: string
  status?: EnrollmentStatus
}): Promise<EnrollmentDetail[]> {
  const query = new URLSearchParams()
  if (params?.studentId) query.set('studentId', params.studentId)
  if (params?.teacherId) query.set('teacherId', params.teacherId)
  if (params?.status) query.set('status', params.status)
  const qs = query.toString()
  return api<EnrollmentDetail[]>(qs ? `/enrollments?${qs}` : '/enrollments')
}

export function fetchEnrollment(id: string): Promise<EnrollmentDetail> {
  return api<EnrollmentDetail>(`/enrollments/${id}`)
}

export function createEnrollment(data: {
  studentId: string
  packageId: string
  teacherId: string
  priceVnd?: number
}): Promise<EnrollmentDetail> {
  return api<EnrollmentDetail>('/enrollments', {
    method: 'POST',
    body: data,
  })
}

export function changeTeacher(
  enrollmentId: string,
  data: { teacherId: string; reason: string },
): Promise<EnrollmentDetail> {
  return api<EnrollmentDetail>(`/enrollments/${enrollmentId}/teacher`, {
    method: 'PUT',
    body: data,
  })
}

export function fetchTeacherHistory(enrollmentId: string): Promise<TeacherAssignmentHistory[]> {
  return api<TeacherAssignmentHistory[]>(`/enrollments/${enrollmentId}/teacher-history`)
}

export function extendEnrollment(
  enrollmentId: string,
  newExpiresAt: string,
): Promise<EnrollmentDetail> {
  return api<EnrollmentDetail>(`/enrollments/${enrollmentId}/extend`, {
    method: 'POST',
    body: { newExpiresAt },
  })
}

export function cancelEnrollment(enrollmentId: string, reason?: string): Promise<void> {
  return api<void>(`/enrollments/${enrollmentId}/cancel`, {
    method: 'POST',
    body: { reason },
  })
}
