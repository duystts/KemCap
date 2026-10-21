import { lazy, Suspense } from 'react'
import type { ReactNode } from 'react'
import { createBrowserRouter } from 'react-router'
import { AppLayout } from './AppLayout'
import { HomeRedirect, RequireRole } from './RequireRole'

// Route lazy để giảm bundle ban đầu (NFR-PERF-07)
const LoginPage = lazy(() => import('@/features/auth/LoginPage'))
const AcceptInvitePage = lazy(() => import('@/features/auth/AcceptInvitePage'))
const ForgotPasswordPage = lazy(() => import('@/features/auth/ForgotPasswordPage'))
const ResetPasswordPage = lazy(() => import('@/features/auth/ResetPasswordPage'))
const PlaceholderPage = lazy(() => import('./PlaceholderPage'))

// Scheduling & Video features (U3)
const AvailabilityEditor = lazy(() => import('@/features/scheduling/AvailabilityEditor'))
const BookSessionPage = lazy(() => import('@/features/scheduling/BookSessionPage'))
const ScheduleCalendar = lazy(() => import('@/features/scheduling/ScheduleCalendar'))
const SessionDetailPage = lazy(() => import('@/features/scheduling/SessionDetailPage'))
const ClassroomPage = lazy(() => import('@/features/classroom/ClassroomPage'))

// User Management, Catalog & Profiles (U2)
const UsersPage = lazy(() => import('@/features/admin/users/UsersPage'))
const CoursesPage = lazy(() => import('@/features/catalog/courses/CoursesPage'))
const PackagesPage = lazy(() => import('@/features/catalog/packages/PackagesPage'))
const StudentsListPage = lazy(() => import('@/features/catalog/profiles/StudentsListPage'))
const StudentProfilePage = lazy(() => import('@/features/catalog/profiles/StudentProfilePage'))
const TeacherProfilePage = lazy(() => import('@/features/catalog/profiles/TeacherProfilePage'))
const EnrollmentDetailPage = lazy(
  () => import('@/features/catalog/enrollments/EnrollmentDetailPage'),
)
const StudentPackagesPage = lazy(() => import('@/features/catalog/enrollments/StudentPackagesPage'))

// Learning & Proficiency (U4)
const StudentLearningPathPage = lazy(
  () => import('@/features/learning/path/StudentLearningPathPage'),
)

function page(node: ReactNode) {
  return <Suspense fallback={<div className="p-8 text-sm">Đang tải…</div>}>{node}</Suspense>
}

const placeholder = (path: string, title: string) => ({
  path,
  element: page(<PlaceholderPage title={title} />),
})

export const router = createBrowserRouter([
  { path: '/', element: <HomeRedirect /> },
  { path: '/login', element: page(<LoginPage />) },
  { path: '/accept-invite', element: page(<AcceptInvitePage />) },
  { path: '/forgot-password', element: page(<ForgotPasswordPage />) },
  { path: '/reset-password', element: page(<ResetPasswordPage />) },
  {
    element: <RequireRole roles={['ADMIN']} />,
    children: [
      {
        path: '/admin',
        element: <AppLayout />,
        children: [
          { index: true, element: page(<PlaceholderPage title="Tổng quan" />) },
          { path: 'users', element: page(<UsersPage />) },
          { path: 'students', element: page(<StudentsListPage />) },
          { path: 'students/:id', element: page(<StudentProfilePage />) },
          { path: 'teachers/:id', element: page(<TeacherProfilePage />) },
          { path: 'courses', element: page(<CoursesPage />) },
          { path: 'packages', element: page(<PackagesPage />) },
          { path: 'enrollments/:id', element: page(<EnrollmentDetailPage />) },
          { path: 'schedule', element: page(<ScheduleCalendar />) },
          placeholder('billing', 'Học phí & công nợ'),
          placeholder('privacy', 'Dữ liệu cá nhân'),
        ],
      },
    ],
  },
  {
    element: <RequireRole roles={['TEACHER']} />,
    children: [
      {
        path: '/teacher',
        element: <AppLayout />,
        children: [
          { index: true, element: page(<PlaceholderPage title="Trang chủ giáo viên" />) },
          { path: 'schedule', element: page(<ScheduleCalendar />) },
          { path: 'availability', element: page(<AvailabilityEditor />) },
          { path: 'students', element: page(<StudentsListPage />) },
          { path: 'students/:id', element: page(<StudentProfilePage />) },
          { path: 'profile', element: page(<TeacherProfilePage />) },
        ],
      },
    ],
  },
  {
    element: <RequireRole roles={['STUDENT']} />,
    children: [
      {
        path: '/student',
        element: <AppLayout />,
        children: [
          { index: true, element: page(<PlaceholderPage title="Trang chủ học viên" />) },
          { path: 'book', element: page(<BookSessionPage />) },
          { path: 'schedule', element: page(<ScheduleCalendar />) },
          { path: 'learning-path', element: page(<StudentLearningPathPage />) },
          placeholder('progress', 'Tiến bộ'),
          { path: 'packages', element: page(<StudentPackagesPage />) },
        ],
      },
    ],
  },
  // Chi tiết buổi học & Lớp học trực tuyến WebRTC
  {
    element: <RequireRole roles={['ADMIN', 'TEACHER', 'STUDENT']} />,
    children: [
      {
        path: '/sessions/:id',
        element: <AppLayout />,
        children: [{ index: true, element: page(<SessionDetailPage />) }],
      },
      {
        path: '/sessions/:id/classroom',
        element: page(<ClassroomPage />),
      },
    ],
  },
  { path: '*', element: <HomeRedirect /> },
])
