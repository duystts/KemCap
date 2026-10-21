import { useAuth } from '@/app/AuthProvider'
import LearningPathView from './LearningPathView'
import GoalsPanel from '../proficiency/GoalsPanel'
import ProficiencyPanel from '../proficiency/ProficiencyPanel'

export default function StudentLearningPathPage() {
  const { me } = useAuth()
  const studentId = me?.id || ''

  if (!studentId) {
    return (
      <div className="p-8 text-center text-sm text-slate-400">Đang tải thông tin học viên…</div>
    )
  }

  return (
    <div className="space-y-8 max-w-7xl mx-auto p-4 sm:p-6">
      <LearningPathView studentId={studentId} canEdit={false} />
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <GoalsPanel studentId={studentId} canEdit={false} />
        <ProficiencyPanel studentId={studentId} canEdit={false} />
      </div>
    </div>
  )
}
