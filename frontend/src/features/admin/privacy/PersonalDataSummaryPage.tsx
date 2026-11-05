import { useState, type ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { useQueries, useQuery } from '@tanstack/react-query'
import { fetchEnrollments, fetchStudentProfile } from '@/lib/api/catalog'
import { fetchStatement } from '@/lib/api/billing'
import { ApiError } from '@/lib/api/client'
import {
  fetchAssignments,
  fetchGoals,
  fetchLearningPath,
  fetchProficiency,
  fetchProgress,
  fetchSessionReport,
  type GoalStatus,
} from '@/lib/api/learning'
import { fetchFeedbackRecord, fetchGuardianConsent } from '@/lib/api/privacy'
import { fetchSessions } from '@/lib/api/scheduling'
import { fetchUser, fetchUsers } from '@/lib/api/users'
import { DateTimeText } from '@/components/DateTimeText'
import { statusLabel } from '@/components/StatusBadge'
import { Button } from '@/components/ui/button'

const GOAL_STATUS_LABEL: Record<GoalStatus, string> = {
  ACTIVE: 'Đang thực hiện',
  ACHIEVED: 'Đã đạt',
  DROPPED: 'Đã bỏ',
}

const formatVnd = (amount?: number | null) =>
  amount == null ? '—' : amount.toLocaleString('vi-VN') + ' đ'

/** Ngày YYYY-MM-DD hiển thị nguyên trạng theo kiểu Việt Nam, không qua múi giờ. */
function formatDay(day?: string | null) {
  if (!day) return '—'
  const [year, month, date] = day.split('-')
  return `${date}/${month}/${year}`
}

/** 404 nghĩa là chưa có dữ liệu (lộ trình, đồng ý giám hộ), không phải lỗi. */
const notFound = (error: unknown) => error instanceof ApiError && error.status === 404

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="bg-white rounded-xl border p-5 space-y-2 break-inside-avoid print:border-0 print:p-0 print:rounded-none">
      <h2 className="text-base text-slate-900">{title}</h2>
      {children}
    </section>
  )
}

function Rows({ rows }: { rows: [string, ReactNode][] }) {
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-1 text-sm">
      {rows.map(([label, value]) => (
        <div key={label} className="contents">
          <dt className="text-slate-500">{label}</dt>
          <dd className="text-slate-900 whitespace-pre-wrap">{value || '—'}</dd>
        </div>
      ))}
    </dl>
  )
}

function Table({
  headers,
  rows,
  empty,
}: {
  headers: string[]
  rows: ReactNode[][]
  empty: string
}) {
  if (rows.length === 0) return <p className="text-sm text-slate-500">{empty}</p>
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="border-b text-xs font-semibold text-slate-500">
          <tr>
            {headers.map((h) => (
              <th key={h} className="py-2 pr-4">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y">
          {rows.map((row, i) => (
            <tr key={i} className="align-top">
              {row.map((cell, j) => (
                <td key={j} className="py-2 pr-4 whitespace-pre-wrap">
                  {cell ?? '—'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default function PersonalDataSummaryPage() {
  const { id } = useParams<{ id: string }>()
  const studentId = id!
  const [generatedAt] = useState(() => new Date().toISOString())

  const user = useQuery({ queryKey: ['user', studentId], queryFn: () => fetchUser(studentId) })
  const profile = useQuery({
    queryKey: ['student-profile', studentId],
    queryFn: () => fetchStudentProfile(studentId),
  })
  const guardian = useQuery({
    queryKey: ['guardian-consent', studentId],
    queryFn: () => fetchGuardianConsent(studentId),
    retry: false,
  })
  const proficiency = useQuery({
    queryKey: ['student-proficiency-summary', studentId],
    queryFn: () => fetchProficiency(studentId),
  })
  const goals = useQuery({
    queryKey: ['goals', studentId],
    queryFn: () => fetchGoals(studentId),
  })
  const path = useQuery({
    queryKey: ['learning-path', studentId],
    queryFn: () => fetchLearningPath(studentId),
    retry: false,
  })
  const enrollments = useQuery({
    queryKey: ['enrollments', 'student', studentId],
    queryFn: () => fetchEnrollments({ studentId }),
  })
  const sessions = useQuery({
    queryKey: ['sessions', 'student', studentId],
    queryFn: () => fetchSessions({ studentId }),
  })
  const progress = useQuery({
    queryKey: ['student-progress', studentId],
    queryFn: () => fetchProgress(studentId),
  })
  const assignments = useQuery({
    queryKey: ['assignments', 'student', studentId],
    queryFn: () => fetchAssignments({ studentId }),
  })
  const teachers = useQuery({
    queryKey: ['users', 'teachers'],
    queryFn: () => fetchUsers({ role: 'TEACHER' }),
  })
  const statements = useQueries({
    queries: (enrollments.data ?? []).map((e) => ({
      queryKey: ['statement', e.id],
      queryFn: () => fetchStatement(e.id),
    })),
  })

  // `progress.recentReports` chỉ có 5 phiếu gần nhất; bản tóm tắt phải đủ mọi nhận xét (FR-031)
  const reports = useQueries({
    queries: (progress.data?.skillSeries ?? []).map((p) => ({
      queryKey: ['session-report', p.sessionId],
      queryFn: () => fetchSessionReport(p.sessionId),
    })),
  })
  const completedSessions = (sessions.data ?? []).filter((s) => s.status === 'COMPLETED')
  const feedback = useQueries({
    queries: completedSessions.map((s) => ({
      queryKey: ['session-feedback', s.id],
      queryFn: () => fetchFeedbackRecord(s.id),
      retry: false,
    })),
  })

  const required = [
    user,
    profile,
    proficiency,
    goals,
    enrollments,
    sessions,
    progress,
    assignments,
    teachers,
    ...statements,
    ...reports,
  ]
  const optional = [guardian, path, ...feedback]
  const loading = required.some((q) => q.isLoading) || optional.some((q) => q.isLoading)
  // Thiếu một nguồn thì bản in không còn đúng là "toàn bộ dữ liệu đang lưu", nên không hiện dở dang
  const failed =
    required.some((q) => q.isError) || optional.some((q) => q.isError && !notFound(q.error))

  const teacherName = (teacherId: string) =>
    teachers.data?.find((t) => t.id === teacherId)?.fullName ?? 'Không rõ tên'

  return (
    <div className="space-y-5 max-w-4xl mx-auto p-4 sm:p-6 print:p-0 print:max-w-none">
      <div className="flex items-center justify-between gap-3 print:hidden">
        <Link to="/admin/privacy" className="text-sm text-slate-600 hover:underline">
          Quay lại danh sách yêu cầu
        </Link>
        <Button type="button" disabled={loading || failed} onClick={() => window.print()}>
          In / lưu PDF
        </Button>
      </div>

      <div>
        <h1 className="text-2xl tracking-tight text-slate-900">Bản tóm tắt dữ liệu cá nhân</h1>
        <p className="text-sm text-slate-500 mt-1">
          Lập lúc <DateTimeText value={generatedAt} />. Liệt kê dữ liệu hệ thống đang lưu về học
          viên.
        </p>
      </div>

      {loading ? (
        <p className="text-sm text-slate-400">Đang tổng hợp dữ liệu…</p>
      ) : failed ? (
        <p role="alert" className="text-sm text-rose-600">
          Không tải đủ dữ liệu nên không lập được bản tóm tắt. Vui lòng thử lại.
        </p>
      ) : (
        <>
          <Section title="Thông tin tài khoản">
            <Rows
              rows={[
                ['Họ và tên', user.data?.fullName],
                ['Email', user.data?.email],
                ['Số điện thoại', user.data?.phone],
                ['Ngày sinh', formatDay(user.data?.dateOfBirth)],
                ['Trạng thái', user.data && statusLabel(user.data.status)],
                ['Nghề nghiệp', profile.data?.occupation],
                ['Trình độ tự khai', profile.data?.selfReportedLevel],
                ['Ghi chú của giáo vụ', profile.data?.notes],
              ]}
            />
          </Section>

          <Section title="Đồng ý của người giám hộ">
            {guardian.data ? (
              <Rows
                rows={[
                  ['Người giám hộ', guardian.data.guardianName],
                  ['Quan hệ', guardian.data.relationship],
                  ['Liên hệ', guardian.data.guardianContact],
                  ['Ngày đồng ý', formatDay(guardian.data.consentedOn)],
                ]}
              />
            ) : (
              <p className="text-sm text-slate-500">Không có.</p>
            )}
          </Section>

          <Section title="Hồ sơ năng lực">
            <Table
              headers={['Ngày', 'Nghe', 'Nói', 'Đọc', 'Viết', 'Ngữ pháp & Từ vựng', 'Ghi chú']}
              empty="Chưa có đánh giá năng lực."
              rows={(proficiency.data?.history ?? []).map((a) => [
                <DateTimeText key="d" value={a.assessedAt} mode="date" />,
                a.listening,
                a.speaking,
                a.reading,
                a.writing,
                a.grammarVocab,
                a.notes,
              ])}
            />
          </Section>

          <Section title="Mục tiêu học tập">
            <Table
              headers={['Loại', 'Mục tiêu', 'Hạn', 'Trạng thái']}
              empty="Chưa có mục tiêu."
              rows={(goals.data ?? []).map((g) => [
                g.type,
                g.target,
                formatDay(g.deadline),
                GOAL_STATUS_LABEL[g.status],
              ])}
            />
          </Section>

          <Section title="Lộ trình học tập">
            {path.data ? (
              <>
                <p className="text-sm text-slate-900">
                  {path.data.title} · hoàn thành {path.data.completionPercent}%
                </p>
                <ul className="text-sm text-slate-700 list-disc pl-5">
                  {path.data.milestones.map((m) => (
                    <li key={m.id}>
                      {m.title}: {m.topics.map((t) => t.title).join(', ') || 'chưa có chủ đề'}
                    </li>
                  ))}
                </ul>
              </>
            ) : (
              <p className="text-sm text-slate-500">Chưa có lộ trình.</p>
            )}
          </Section>

          <Section title="Ghi danh và thanh toán">
            <Table
              headers={[
                'Gói',
                'Giáo viên',
                'Trạng thái',
                'Hết hạn',
                'Phải thu',
                'Đã thu',
                'Còn nợ',
              ]}
              empty="Chưa có ghi danh."
              rows={(enrollments.data ?? []).map((e, i) => {
                const statement = statements[i]?.data
                return [
                  `${e.sessionsTotal} buổi × ${e.sessionMinutes} phút`,
                  teacherName(e.teacherId),
                  statusLabel(e.status),
                  e.expiresAt ? <DateTimeText key="x" value={e.expiresAt} mode="date" /> : '—',
                  formatVnd(statement?.amountDueVnd),
                  formatVnd(statement?.amountPaidVnd),
                  formatVnd(statement?.debtVnd),
                ]
              })}
            />
            <Table
              headers={['Ngày', 'Số tiền', 'Ghi chú']}
              empty="Chưa có khoản thanh toán."
              rows={statements
                .flatMap((s) => s.data?.payments ?? [])
                .map((p) => [formatDay(p.paidAt), formatVnd(p.amountVnd), p.note])}
            />
          </Section>

          <Section title="Buổi học">
            <Table
              headers={['Thời gian', 'Giáo viên', 'Trạng thái']}
              empty="Chưa có buổi học."
              rows={(sessions.data ?? []).map((s) => [
                <DateTimeText key="t" value={s.startsAt} />,
                teacherName(s.teacherId),
                statusLabel(s.status),
              ])}
            />
          </Section>

          <Section title="Nhận xét và điểm kỹ năng">
            <Table
              headers={['Ngày', 'Nghe', 'Nói', 'Đọc', 'Viết', 'NP & TV', 'Nhận xét']}
              empty="Chưa có phiếu đánh giá."
              rows={(progress.data?.skillSeries ?? []).map((p) => [
                formatDay(p.date),
                p.listening,
                p.speaking,
                p.reading,
                p.writing,
                p.grammarVocab,
                reports.find((r) => r.data?.sessionId === p.sessionId)?.data?.comment,
              ])}
            />
          </Section>

          <Section title="Đánh giá sao của học viên">
            <Table
              headers={['Buổi', 'Số sao', 'Nhận xét']}
              empty="Chưa có đánh giá."
              rows={completedSessions.flatMap((s, i) => {
                const record = feedback[i]?.data
                return record
                  ? [[<DateTimeText key="t" value={s.startsAt} />, record.stars, record.comment]]
                  : []
              })}
            />
          </Section>

          <Section title="Bài tập và bài nộp">
            <Table
              headers={['Bài tập', 'Bài nộp', 'Điểm', 'Phản hồi']}
              empty="Chưa có bài tập."
              rows={(assignments.data ?? []).map((a) => [
                a.title,
                a.submission?.textAnswer,
                a.submission?.score,
                a.submission?.feedback,
              ])}
            />
          </Section>
        </>
      )}
    </div>
  )
}
