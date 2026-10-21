import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  fetchUsers,
  updateUserStatus,
  resendInvite,
  type UserItem,
  type UserStatus,
} from '@/lib/api/users'
import type { Role } from '@/lib/api/types'
import { StatusBadge } from '@/components/StatusBadge'
import CreateUserDialog from './CreateUserDialog'
import GuardianConsentDialog from './GuardianConsentDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import {
  Users,
  UserPlus,
  Search,
  Mail,
  Shield,
  RotateCw,
  ExternalLink,
  Power,
  Calendar,
} from 'lucide-react'

const ROLE_LABELS: Record<Role, { label: string; color: string }> = {
  ADMIN: { label: 'Giáo vụ / Admin', color: 'bg-purple-100 text-purple-800' },
  TEACHER: { label: 'Giáo viên', color: 'bg-indigo-100 text-indigo-800' },
  STUDENT: { label: 'Học viên', color: 'bg-teal-100 text-teal-800' },
}

export default function UsersPage() {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [search, setSearch] = useState('')
  const [selectedRole, setSelectedRole] = useState<Role | ''>('')
  const [selectedStatus, setSelectedStatus] = useState<UserStatus | ''>('')

  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [consentStudent, setConsentStudent] = useState<{ id: string; name: string } | null>(null)

  const { data: users = [], isLoading } = useQuery({
    queryKey: ['users', selectedRole, selectedStatus, search],
    queryFn: () =>
      fetchUsers({
        role: selectedRole || undefined,
        status: selectedStatus || undefined,
        q: search || undefined,
      }),
  })

  // Mutation đổi trạng thái Active / Inactive
  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: 'ACTIVE' | 'INACTIVE' }) =>
      updateUserStatus(id, status),
    onSuccess: (updatedUser) => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      toast.success(
        updatedUser.status === 'ACTIVE'
          ? 'Đã kích hoạt lại người dùng!'
          : 'Đã tạm ngừng người dùng!',
      )
    },
    onError: (err: any) => {
      if (err?.code === 'TEACHER_HAS_STUDENTS') {
        toast.error('Giáo viên còn học viên được phân công — hãy đổi giáo viên trước.')
      } else {
        showError(err)
      }
    },
  })

  // Mutation gửi lại lời mời
  const resendMutation = useMutation({
    mutationFn: (id: string) => resendInvite(id),
    onSuccess: () => {
      toast.success('Đã gửi lại thư mời kích hoạt tài khoản thành công!')
    },
    onError: (err) => showError(err),
  })

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Users className="w-6 h-6 text-indigo-600" />
            Quản lý người dùng
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Danh sách tài khoản quản trị viên, giáo viên và học viên trong hệ thống.
          </p>
        </div>
        <Button onClick={() => setCreateDialogOpen(true)} className="shrink-0">
          <UserPlus className="w-4 h-4 mr-2" />
          Thêm người dùng
        </Button>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-2xs flex flex-col md:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <Input
            placeholder="Tìm theo tên hoặc email…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
        <div className="flex flex-wrap sm:flex-nowrap gap-2">
          <select
            value={selectedRole}
            onChange={(e) => setSelectedRole(e.target.value as Role | '')}
            className="px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
          >
            <option value="">Tất cả vai trò</option>
            <option value="ADMIN">Giáo vụ / Admin</option>
            <option value="TEACHER">Giáo viên</option>
            <option value="STUDENT">Học viên</option>
          </select>

          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value as UserStatus | '')}
            className="px-3 py-2 text-sm border rounded-lg focus:outline-hidden focus:ring-2 focus:ring-indigo-500 bg-white"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang dùng</option>
            <option value="INVITED">Đã mời</option>
            <option value="AWAITING_GUARDIAN_CONSENT">Chờ người giám hộ</option>
            <option value="INACTIVE">Ngừng kích hoạt</option>
          </select>
        </div>
      </div>

      {/* User Table */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-2xs">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-slate-400">Đang tải người dùng…</div>
        ) : users.length === 0 ? (
          <div className="p-12 text-center text-sm text-slate-500">
            Không tìm thấy người dùng nào phù hợp.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b text-xs uppercase font-semibold text-slate-500">
                <tr>
                  <th className="py-3 px-4">Người dùng</th>
                  <th className="py-3 px-4">Vai trò</th>
                  <th className="py-3 px-4">Trạng thái</th>
                  <th className="py-3 px-4">Ngày sinh</th>
                  <th className="py-3 px-4">Lần đăng nhập cuối</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {users.map((u: UserItem) => {
                  const roleConfig = ROLE_LABELS[u.role] || {
                    label: u.role,
                    color: 'bg-slate-100 text-slate-800',
                  }
                  return (
                    <tr key={u.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4">
                        <div className="font-semibold text-slate-900">{u.fullName}</div>
                        <div className="text-xs text-slate-500 flex items-center gap-1.5 mt-0.5">
                          <Mail className="w-3.5 h-3.5 text-slate-400" />
                          {u.email}
                          {u.phone && <span className="text-slate-400">• {u.phone}</span>}
                        </div>
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${roleConfig.color}`}
                        >
                          {roleConfig.label}
                        </span>
                      </td>
                      <td className="py-3.5 px-4">
                        <StatusBadge status={u.status} />
                      </td>
                      <td className="py-3.5 px-4 text-xs text-slate-600">
                        {u.dateOfBirth ? (
                          <span className="flex items-center gap-1">
                            <Calendar className="w-3.5 h-3.5 text-slate-400" />
                            {new Date(u.dateOfBirth).toLocaleDateString('vi-VN')}
                          </span>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-xs text-slate-500">
                        {u.lastLoginAt ? (
                          new Date(u.lastLoginAt).toLocaleString('vi-VN', {
                            day: '2-digit',
                            month: '2-digit',
                            year: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                          })
                        ) : (
                          <span className="text-slate-400">Chưa đăng nhập</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {/* Hồ sơ học viên / giáo viên */}
                          {u.role === 'STUDENT' && (
                            <Link
                              to={`/admin/students/${u.id}`}
                              className="p-1.5 text-slate-600 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                              title="Xem hồ sơ học viên"
                            >
                              <ExternalLink className="w-4 h-4" />
                            </Link>
                          )}
                          {u.role === 'TEACHER' && (
                            <Link
                              to={`/admin/teachers/${u.id}`}
                              className="p-1.5 text-slate-600 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                              title="Xem hồ sơ giáo viên"
                            >
                              <ExternalLink className="w-4 h-4" />
                            </Link>
                          )}

                          {/* Nhập đồng ý phụ huynh nếu < 16 tuổi */}
                          {u.status === 'AWAITING_GUARDIAN_CONSENT' && (
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => setConsentStudent({ id: u.id, name: u.fullName })}
                              className="h-8 text-xs border-amber-300 text-amber-700 bg-amber-50 hover:bg-amber-100"
                            >
                              <Shield className="w-3.5 h-3.5 mr-1 text-amber-600" />
                              Người giám hộ
                            </Button>
                          )}

                          {/* Gửi lại lời mời */}
                          {u.status === 'INVITED' && (
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() => resendMutation.mutate(u.id)}
                              disabled={resendMutation.isPending}
                              className="h-8 text-xs text-slate-600 hover:text-indigo-600"
                              title="Gửi lại email mời"
                            >
                              <RotateCw className="w-3.5 h-3.5 mr-1" />
                              Gửi lại mời
                            </Button>
                          )}

                          {/* Ngừng / Kích hoạt lại */}
                          {(u.status === 'ACTIVE' || u.status === 'INACTIVE') && (
                            <Button
                              size="sm"
                              variant="ghost"
                              onClick={() =>
                                statusMutation.mutate({
                                  id: u.id,
                                  status: u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
                                })
                              }
                              disabled={statusMutation.isPending}
                              className={`h-8 text-xs ${
                                u.status === 'ACTIVE'
                                  ? 'text-red-600 hover:bg-red-50'
                                  : 'text-emerald-600 hover:bg-emerald-50'
                              }`}
                              title={u.status === 'ACTIVE' ? 'Ngừng kích hoạt' : 'Kích hoạt lại'}
                            >
                              <Power className="w-3.5 h-3.5 mr-1" />
                              {u.status === 'ACTIVE' ? 'Ngừng' : 'Mở lại'}
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Dialogs */}
      <CreateUserDialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} />
      <GuardianConsentDialog
        studentId={consentStudent?.id || null}
        studentName={consentStudent?.name}
        open={!!consentStudent}
        onClose={() => setConsentStudent(null)}
      />
    </div>
  )
}
