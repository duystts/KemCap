import { useState } from 'react'
import { useParams, Link } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '@/app/AuthProvider'
import {
  fetchStudentProfile,
  updateStudentProfile,
  fetchEnrollments,
  type EnrollmentDetail,
  type CefrLevel,
} from '@/lib/api/catalog'
import { fetchUser } from '@/lib/api/users'
import { StatusBadge } from '@/components/StatusBadge'
import ProficiencyPanel from '@/features/learning/proficiency/ProficiencyPanel'
import GoalsPanel from '@/features/learning/proficiency/GoalsPanel'
import LearningPathView from '@/features/learning/path/LearningPathView'
import CreateEnrollmentDialog from '@/features/catalog/enrollments/CreateEnrollmentDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import {
  User,
  Mail,
  Calendar,
  Award,
  BookOpen,
  Edit,
  Plus,
  ExternalLink,
  X,
  CreditCard,
  Briefcase,
  FileText,
} from 'lucide-react'

type TabType = 'info' | 'proficiency' | 'path' | 'enrollments'

const CEFR_LEVELS: CefrLevel[] = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2']

export default function StudentProfilePage() {
  const { id } = useParams<{ id: string }>()
  const { me } = useAuth()
  const isAdmin = me?.role === 'ADMIN'
  const isTeacher = me?.role === 'TEACHER'
  const canEdit = isAdmin || isTeacher

  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [activeTab, setActiveTab] = useState<TabType>('info')
  const [editProfileOpen, setEditProfileOpen] = useState(false)
  const [createEnrollmentOpen, setCreateEnrollmentOpen] = useState(false)

  // Form edit states
  const [selfReportedLevel, setSelfReportedLevel] = useState<CefrLevel | ''>('')
  const [occupation, setOccupation] = useState('')
  const [notes, setNotes] = useState('')

  // Queries
  const { data: user } = useQuery({
    queryKey: ['user', id],
    queryFn: () => fetchUser(id!),
    enabled: !!id,
  })

  const { data: profile, isLoading: loadingProfile } = useQuery({
    queryKey: ['student-profile', id],
    queryFn: () => fetchStudentProfile(id!),
    enabled: !!id,
  })

  const { data: enrollments = [] } = useQuery({
    queryKey: ['enrollments', 'student', id],
    queryFn: () => fetchEnrollments({ studentId: id }),
    enabled: !!id,
  })

  const updateProfileMutation = useMutation({
    mutationFn: () =>
      updateStudentProfile(id!, {
        selfReportedLevel: selfReportedLevel ? (selfReportedLevel as CefrLevel) : undefined,
        occupation: occupation.trim() || undefined,
        notes: isAdmin ? notes.trim() || undefined : undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student-profile', id] })
      queryClient.invalidateQueries({ queryKey: ['students'] })
      toast.success('Đã cập nhật hồ sơ học viên thành công!')
      setEditProfileOpen(false)
    },
    onError: (err) => showError(err),
  })

  const openEditModal = () => {
    setSelfReportedLevel(profile?.selfReportedLevel || '')
    setOccupation(profile?.occupation || '')
    setNotes(profile?.notes || '')
    setEditProfileOpen(true)
  }

  if (loadingProfile) {
    return <div className="p-12 text-center text-sm text-slate-400">Đang tải hồ sơ học viên…</div>
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      {/* Student Banner Header */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-2xs">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-start gap-4">
            <div className="w-16 h-16 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
              <User className="w-8 h-8" />
            </div>
            <div>
              <div className="flex items-center gap-2 mb-1">
                <h1 className="text-2xl font-bold text-slate-900">
                  {user?.fullName || 'Hồ sơ học viên'}
                </h1>
                {user && <StatusBadge status={user.status} />}
              </div>
              <div className="flex flex-wrap items-center gap-3 text-xs text-slate-500">
                <span className="flex items-center gap-1">
                  <Mail className="w-3.5 h-3.5" />
                  {user?.email}
                </span>
                {user?.phone && <span>• {user.phone}</span>}
                {user?.dateOfBirth && (
                  <span className="flex items-center gap-1">
                    • <Calendar className="w-3.5 h-3.5" />
                    {new Date(user.dateOfBirth).toLocaleDateString('vi-VN')}
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {canEdit && (
              <Button variant="outline" size="sm" onClick={openEditModal}>
                <Edit className="w-4 h-4 mr-1.5" />
                Cập nhật thông tin
              </Button>
            )}
            {isAdmin && (
              <Button size="sm" onClick={() => setCreateEnrollmentOpen(true)}>
                <Plus className="w-4 h-4 mr-1.5" />
                Tạo ghi danh
              </Button>
            )}
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200 mt-6 -mb-6 gap-6 text-sm font-semibold">
          <button
            type="button"
            onClick={() => setActiveTab('info')}
            className={`pb-3 border-b-2 transition-colors flex items-center gap-2 ${
              activeTab === 'info'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <User className="w-4 h-4" />
            Thông tin chung
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('proficiency')}
            className={`pb-3 border-b-2 transition-colors flex items-center gap-2 ${
              activeTab === 'proficiency'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Award className="w-4 h-4" />
            Năng lực & Mục tiêu
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('path')}
            className={`pb-3 border-b-2 transition-colors flex items-center gap-2 ${
              activeTab === 'path'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <BookOpen className="w-4 h-4" />
            Lộ trình học tập
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('enrollments')}
            className={`pb-3 border-b-2 transition-colors flex items-center gap-2 ${
              activeTab === 'enrollments'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <CreditCard className="w-4 h-4" />
            Gói học & Ghi danh ({enrollments.length})
          </button>
        </div>
      </div>

      {/* Tab 1: General Info */}
      {activeTab === 'info' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-bold text-slate-800">
                Thông tin trình độ & Công việc
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-sm">
              <div className="flex justify-between py-2 border-b">
                <span className="text-slate-500 flex items-center gap-1.5">
                  <Award className="w-4 h-4 text-indigo-600" />
                  Trình độ tự khai:
                </span>
                <span className="font-bold text-indigo-700">
                  {profile?.selfReportedLevel
                    ? `CEFR ${profile.selfReportedLevel}`
                    : 'Chưa khai báo'}
                </span>
              </div>
              <div className="flex justify-between py-2 border-b">
                <span className="text-slate-500 flex items-center gap-1.5">
                  <Briefcase className="w-4 h-4 text-slate-500" />
                  Nghề nghiệp / Lĩnh vực:
                </span>
                <span className="font-semibold text-slate-800">
                  {profile?.occupation || 'Chưa cập nhật'}
                </span>
              </div>
            </CardContent>
          </Card>

          {isAdmin && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-bold text-slate-800 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-slate-500" />
                  Ghi chú nội bộ giáo vụ
                </CardTitle>
              </CardHeader>
              <CardContent>
                {profile?.notes ? (
                  <p className="text-sm text-slate-700 whitespace-pre-wrap bg-slate-50 p-4 rounded-xl border">
                    {profile.notes}
                  </p>
                ) : (
                  <p className="text-xs text-slate-400 italic">
                    Chưa có ghi chú nội bộ về học viên này.
                  </p>
                )}
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {/* Tab 2: CEFR Proficiency & Goals */}
      {activeTab === 'proficiency' && (
        <div className="space-y-6">
          <GoalsPanel studentId={id!} canEdit={canEdit} />
          <ProficiencyPanel studentId={id!} canEdit={canEdit} />
        </div>
      )}

      {/* Tab 3: Learning Path */}
      {activeTab === 'path' && <LearningPathView studentId={id!} canEdit={canEdit} />}

      {/* Tab 4: Enrollments */}
      {activeTab === 'enrollments' && (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <CardTitle className="text-base font-bold text-slate-800">
              Danh sách gói học đã ghi danh
            </CardTitle>
            {isAdmin && (
              <Button size="sm" onClick={() => setCreateEnrollmentOpen(true)}>
                <Plus className="w-4 h-4 mr-1.5" />
                Tạo ghi danh mới
              </Button>
            )}
          </CardHeader>
          <CardContent>
            {enrollments.length === 0 ? (
              <div className="p-8 text-center text-sm text-slate-500 bg-slate-50 rounded-xl border border-dashed">
                Học viên chưa có gói học nào.
              </div>
            ) : (
              <div className="space-y-3">
                {enrollments.map((enr: EnrollmentDetail) => (
                  <div
                    key={enr.id}
                    className="p-4 rounded-xl border border-slate-200 bg-white flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:border-indigo-200 transition-colors"
                  >
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-bold text-slate-900 text-sm">
                          Gói {enr.sessionsTotal} buổi ({enr.sessionMinutes}p)
                        </span>
                        <StatusBadge status={enr.status} />
                      </div>
                      <div className="text-xs text-slate-500 flex flex-wrap gap-3">
                        <span>
                          Đã học: <strong>{enr.sessionsConsumed}</strong>
                        </span>
                        <span>
                          Đã đặt: <strong>{enr.sessionsReserved}</strong>
                        </span>
                        <span>
                          Còn lại:{' '}
                          <strong className="text-indigo-600">{enr.sessionsRemaining}</strong>
                        </span>
                        {enr.expiresAt && (
                          <span>Hạn: {new Date(enr.expiresAt).toLocaleDateString('vi-VN')}</span>
                        )}
                      </div>
                    </div>

                    <Link
                      to={`/admin/enrollments/${enr.id}`}
                      className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 self-start sm:self-auto"
                    >
                      Chi tiết & Học phí
                      <ExternalLink className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Dialog Cập nhật thông tin học viên */}
      {editProfileOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-2 border-b">
              <h3 className="text-base font-bold text-slate-800">Cập nhật hồ sơ học viên</h3>
              <button
                type="button"
                onClick={() => setEditProfileOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                updateProfileMutation.mutate()
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Trình độ CEFR tự khai
                </label>
                <select
                  value={selfReportedLevel}
                  onChange={(e) => setSelfReportedLevel(e.target.value as CefrLevel | '')}
                  className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
                >
                  <option value="">-- Chưa xác định --</option>
                  {CEFR_LEVELS.map((lvl) => (
                    <option key={lvl} value={lvl}>
                      CEFR {lvl}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Nghề nghiệp / Lĩnh vực
                </label>
                <Input
                  placeholder="Ví dụ: Sinh viên, Lập trình viên, Nhân sự…"
                  value={occupation}
                  onChange={(e) => setOccupation(e.target.value)}
                />
              </div>

              {isAdmin && (
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Ghi chú nội bộ giáo vụ
                  </label>
                  <textarea
                    rows={3}
                    placeholder="Ghi chú về thói quen học, yêu cầu đặc biệt của học viên…"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
                  />
                </div>
              )}

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button type="button" variant="outline" onClick={() => setEditProfileOpen(false)}>
                  Hủy
                </Button>
                <Button type="submit" disabled={updateProfileMutation.isPending}>
                  {updateProfileMutation.isPending ? 'Đang lưu…' : 'Lưu hồ sơ'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Dialog Tạo ghi danh mới */}
      <CreateEnrollmentDialog
        defaultStudentId={id}
        open={createEnrollmentOpen}
        onClose={() => setCreateEnrollmentOpen(false)}
      />
    </div>
  )
}
