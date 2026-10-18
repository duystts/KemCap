import { Navigate, Outlet, useLocation } from 'react-router'
import type { Role } from '@/lib/api/types'
import { ConsentGatePage } from '@/features/auth/ConsentGatePage'
import { useAuth } from './AuthProvider'

export const HOME_BY_ROLE: Record<Role, string> = {
  ADMIN: '/admin',
  TEACHER: '/teacher',
  STUDENT: '/student',
}

/** Chặn route theo vai trò (chỉ là lớp giao diện; backend mới là lớp bảo vệ — constitution V). */
export function RequireRole({ roles }: { roles: Role[] }) {
  const { status, me } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <div className="p-8 text-sm text-muted-foreground">Đang tải…</div>
  }
  if (status === 'anonymous' || !me) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (me.consentRequired) {
    return <ConsentGatePage />
  }
  if (!roles.includes(me.role)) {
    return <Navigate to={HOME_BY_ROLE[me.role]} replace />
  }
  return <Outlet />
}

export function HomeRedirect() {
  const { status, me } = useAuth()
  if (status === 'loading') return null
  if (!me) return <Navigate to="/login" replace />
  return <Navigate to={HOME_BY_ROLE[me.role]} replace />
}
