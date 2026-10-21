import { useState, useEffect } from 'react'
import { useParams } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useAuth } from '@/app/AuthProvider'
import { fetchTeacherProfile, updateTeacherProfile } from '@/lib/api/catalog'
import { fetchUser } from '@/lib/api/users'
import { StatusBadge } from '@/components/StatusBadge'
import AvailabilityEditor from '@/features/scheduling/AvailabilityEditor'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { useApiErrorToast } from '@/components/useApiErrorToast'
import { User, Mail, Award, Edit, Clock, X } from 'lucide-react'

export default function TeacherProfilePage() {
  const { id: paramId } = useParams<{ id: string }>()
  const { me } = useAuth()
  const teacherId = paramId || me?.id || ''

  const isAdmin = me?.role === 'ADMIN'
  const isSelf = me?.id === teacherId
  const canEdit = isAdmin || isSelf

  const queryClient = useQueryClient()
  const showError = useApiErrorToast()

  const [activeTab, setActiveTab] = useState<'profile' | 'availability'>('profile')
  const [editDialogOpen, setEditDialogOpen] = useState(false)

  // Form states
  const [bio, setBio] = useState('')
  const [specialtiesText, setSpecialtiesText] = useState('')
  const [certificates, setCertificates] = useState('')

  const { data: user } = useQuery({
    queryKey: ['user', teacherId],
    queryFn: () => fetchUser(teacherId),
    enabled: !!teacherId,
  })

  const { data: profile, isLoading } = useQuery({
    queryKey: ['teacher-profile', teacherId],
    queryFn: () => fetchTeacherProfile(teacherId),
    enabled: !!teacherId,
  })

  useEffect(() => {
    if (profile) {
      setBio(profile.bio || '')
      setSpecialtiesText(profile.specialties?.join(', ') || '')
      setCertificates(profile.certificates || '')
    }
  }, [profile])

  const updateMutation = useMutation({
    mutationFn: () => {
      const specialties = specialtiesText
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
      return updateTeacherProfile(teacherId, {
        bio: bio.trim() || undefined,
        specialties,
        certificates: certificates.trim() || undefined,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teacher-profile', teacherId] })
      toast.success('Đã cập nhật hồ sơ giáo viên thành công!')
      setEditDialogOpen(false)
    },
    onError: (err) => showError(err),
  })

  if (isLoading) {
    return <div className="p-12 text-center text-sm text-slate-400">Đang tải hồ sơ giáo viên…</div>
  }

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 sm:p-6">
      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-2xs">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-start gap-4">
            <div className="w-16 h-16 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
              <User className="w-8 h-8" />
            </div>
            <div>
              <div className="flex items-center gap-2 mb-1">
                <h1 className="text-2xl font-bold text-slate-900">
                  {user?.fullName || 'Hồ sơ giáo viên'}
                </h1>
                {user && <StatusBadge status={user.status} />}
              </div>
              <div className="flex flex-wrap items-center gap-3 text-xs text-slate-500">
                <span className="flex items-center gap-1">
                  <Mail className="w-3.5 h-3.5" />
                  {user?.email}
                </span>
                {user?.phone && <span>• {user.phone}</span>}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {canEdit && (
              <Button variant="outline" size="sm" onClick={() => setEditDialogOpen(true)}>
                <Edit className="w-4 h-4 mr-1.5" />
                Cập nhật thông tin
              </Button>
            )}
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200 mt-6 -mb-6 gap-6 text-sm font-semibold">
          <button
            type="button"
            onClick={() => setActiveTab('profile')}
            className={`pb-3 border-b-2 transition-colors flex items-center gap-2 ${
              activeTab === 'profile'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <User className="w-4 h-4" />
            Thông tin chuyên môn
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('availability')}
            className={`pb-3 border-b-2 transition-colors flex items-center gap-2 ${
              activeTab === 'availability'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Clock className="w-4 h-4" />
            Lịch rảnh & Ngày nghỉ
          </button>
        </div>
      </div>

      {/* Tab 1: Profile Details */}
      {activeTab === 'profile' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-bold text-slate-800 flex items-center gap-2">
                <Award className="w-4 h-4 text-indigo-600" />
                Chuyên môn & Lĩnh vực giảng dạy
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block mb-2">
                  Lĩnh vực thế mạnh
                </span>
                {profile?.specialties && profile.specialties.length > 0 ? (
                  <div className="flex flex-wrap gap-2">
                    {profile.specialties.map((spec: string, idx: number) => (
                      <span
                        key={idx}
                        className="inline-flex items-center px-3 py-1 rounded-lg text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200"
                      >
                        {spec}
                      </span>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-slate-400 italic">Chưa cập nhật chuyên môn.</p>
                )}
              </div>

              <div className="pt-3 border-t">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block mb-1">
                  Chứng chỉ sư phạm & Ngoại ngữ
                </span>
                <p className="text-sm font-medium text-slate-800">
                  {profile?.certificates || (
                    <span className="text-slate-400 italic font-normal text-xs">
                      Chưa cập nhật chứng chỉ.
                    </span>
                  )}
                </p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-bold text-slate-800">
                Giới thiệu bản thân (Bio)
              </CardTitle>
            </CardHeader>
            <CardContent>
              {profile?.bio ? (
                <p className="text-sm text-slate-700 whitespace-pre-wrap bg-slate-50 p-4 rounded-xl border leading-relaxed">
                  {profile.bio}
                </p>
              ) : (
                <p className="text-xs text-slate-400 italic">Chưa có bài viết giới thiệu.</p>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab 2: Availability Editor */}
      {activeTab === 'availability' && <AvailabilityEditor teacherIdOverride={teacherId} />}

      {/* Dialog Chỉnh sửa hồ sơ giáo viên */}
      {editDialogOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-2 border-b">
              <h3 className="text-base font-bold text-slate-800">Cập nhật hồ sơ giáo viên</h3>
              <button
                type="button"
                onClick={() => setEditDialogOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault()
                updateMutation.mutate()
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Chuyên môn (phân cách bằng dấu phẩy)
                </label>
                <Input
                  placeholder="IELTS Speaking, Giao tiếp thương mại, Phát âm…"
                  value={specialtiesText}
                  onChange={(e) => setSpecialtiesText(e.target.value)}
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Chứng chỉ sư phạm / Ngoại ngữ
                </label>
                <Input
                  placeholder="IELTS 8.5, CELTA Grade A, TESOL…"
                  value={certificates}
                  onChange={(e) => setCertificates(e.target.value)}
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Giới thiệu bản thân (Bio)
                </label>
                <textarea
                  rows={4}
                  placeholder="Kinh nghiệm giảng dạy, triết lý sư phạm, phong cách giảng dạy…"
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  className="w-full px-3 py-2 text-sm border rounded-lg bg-white"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t">
                <Button type="button" variant="outline" onClick={() => setEditDialogOpen(false)}>
                  Hủy
                </Button>
                <Button type="submit" disabled={updateMutation.isPending}>
                  {updateMutation.isPending ? 'Đang lưu…' : 'Lưu hồ sơ'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
