import { NavLink, Outlet } from 'react-router'
import { LogOut } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { NotificationBell } from '@/features/notifications/NotificationBell'
import type { Role } from '@/lib/api/types'
import { cn } from '@/lib/utils'
import { useAuth } from './AuthProvider'

interface NavItem {
  to: string
  label: string
}

// Mục menu theo vai trò; các unit sau thêm route tương ứng.
const NAV: Record<Role, NavItem[]> = {
  ADMIN: [
    { to: '/admin', label: 'Tổng quan' },
    { to: '/admin/users', label: 'Người dùng' },
    { to: '/admin/students', label: 'Học viên' },
    { to: '/admin/courses', label: 'Khóa học & gói' },
    { to: '/admin/schedule', label: 'Lịch học' },
    { to: '/admin/billing', label: 'Học phí' },
    { to: '/admin/privacy', label: 'Dữ liệu cá nhân' },
  ],
  TEACHER: [
    { to: '/teacher', label: 'Trang chủ' },
    { to: '/teacher/schedule', label: 'Lịch dạy' },
    { to: '/teacher/availability', label: 'Lịch rảnh' },
    { to: '/teacher/students', label: 'Học viên của tôi' },
  ],
  STUDENT: [
    { to: '/student', label: 'Trang chủ' },
    { to: '/student/book', label: 'Đặt lịch' },
    { to: '/student/schedule', label: 'Lịch học' },
    { to: '/student/learning-path', label: 'Lộ trình' },
    { to: '/student/progress', label: 'Tiến bộ' },
    { to: '/student/packages', label: 'Gói học' },
  ],
}

const ROLE_LABEL: Record<Role, string> = {
  ADMIN: 'Giáo vụ',
  TEACHER: 'Giáo viên',
  STUDENT: 'Học viên',
}

export function AppLayout() {
  const { me, logout } = useAuth()
  if (!me) return null
  return (
    <div className="flex min-h-full flex-col md:flex-row">
      <aside className="border-b border-border bg-muted md:w-60 md:border-b-0 md:border-r">
        <div className="px-4 py-4">
          <p className="text-base font-semibold">English 1-1</p>
          <p className="text-xs text-muted-foreground">{ROLE_LABEL[me.role]}</p>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-2 pb-2 md:flex-col md:pb-4">
          {NAV[me.role].map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to.split('/').length === 2}
              className={({ isActive }) =>
                cn(
                  'whitespace-nowrap rounded-md px-3 py-2 text-sm',
                  isActive ? 'bg-white font-medium text-primary shadow-sm' : 'hover:bg-white/60',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="flex flex-1 flex-col">
        <header className="flex items-center justify-end gap-3 border-b border-border px-4 py-3">
          <NotificationBell />
          <span className="text-sm font-medium">{me.fullName}</span>
          <Button variant="ghost" size="sm" onClick={() => void logout()} aria-label="Đăng xuất">
            <LogOut className="h-4 w-4" />
            Đăng xuất
          </Button>
        </header>
        <main className="flex-1 p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
