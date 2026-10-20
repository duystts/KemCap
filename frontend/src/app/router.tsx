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
          placeholder('users', 'Người dùng'),
          placeholder('students', 'Học viên'),
          placeholder('courses', 'Khóa học & gói học'),
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
          placeholder('students', 'Học viên của tôi'),
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
          placeholder('learning-path', 'Lộ trình'),
          placeholder('progress', 'Tiến bộ'),
          placeholder('packages', 'Gói học'),
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
