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

// Catalog, Users & Enrollments features (U2)
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
          { index: true, element: page(<PlaceholderPage title="Tổng quan" />) },
          { path: 'availability', element: page(<AvailabilityEditor />) },
          { path: 'schedule', element: page(<ScheduleCalendar />) },
          { path: 'students', element: page(<StudentsListPage />) },
          { path: 'students/:id', element: page(<StudentProfilePage />) },
          placeholder('evaluations', 'Đánh giá buổi học'),
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
          { index: true, element: page(<PlaceholderPage title="Tổng quan" />) },
          { path: 'packages', element: page(<StudentPackagesPage />) },
          { path: 'book', element: page(<BookSessionPage />) },
          { path: 'schedule', element: page(<ScheduleCalendar />) },
          placeholder('learning-path', 'Lộ trình học'),
          placeholder('evaluations', 'Tiến bộ & Nhận xét'),
        ],
      },
    ],
  },
  // Các trang dùng chung (chi tiết buổi, phòng học)
  {
    element: <RequireRole roles={['ADMIN', 'TEACHER', 'STUDENT']} />,
    children: [
      {
        element: <AppLayout />,
        children: [{ path: '/sessions/:id', element: page(<SessionDetailPage />) }],
      },
      {
        path: '/classroom/:id',
        element: page(<ClassroomPage />),
      },
    ],
  },
  { path: '*', element: page(<PlaceholderPage title="Không tìm thấy trang" />) },
])
