import { useState } from 'react'
import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  anonymizeStudent,
  completeAccessRequest,
  createDataRequest,
  fetchDataRequests,
  rejectDataRequest,
  type DataRequestItem,
  type DataRequestType,
} from '@/lib/api/privacy'
import { fetchUsers } from '@/lib/api/users'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DateTimeText } from '@/components/DateTimeText'
import { StatusBadge } from '@/components/StatusBadge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useApiErrorToast } from '@/components/useApiErrorToast'

const TYPE_LABEL: Record<DataRequestType, string> = {
  ACCESS: 'Xem dữ liệu',
  ERASURE: 'Xóa dữ liệu',
}

const MAX_REQUESTER = 150
const MAX_NOTE = 2000

// Không dùng toISOString: trước 7h sáng sẽ lệch về hôm trước
function today() {
  const d = new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

function CreateRequestForm({ students }: { students: { id: string; fullName: string }[] }) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()
  const [studentId, setStudentId] = useState('')
  const [requesterName, setRequesterName] = useState('')
  const [type, setType] = useState<DataRequestType>('ACCESS')
  const [maxDate] = useState(today)
  const [receivedOn, setReceivedOn] = useState(maxDate)

  const createMutation = useMutation({
    mutationFn: () =>
      createDataRequest({ studentId, requesterName: requesterName.trim(), type, receivedOn }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['data-subject-requests'] })
      toast.success('Đã ghi nhận yêu cầu!')
      setStudentId('')
      setRequesterName('')
      setType('ACCESS')
      setReceivedOn(maxDate)
    },
    onError: (err) => showError(err),
  })

  return (
    <section className="bg-white rounded-xl border p-5 space-y-3">
      <h2 className="text-base text-slate-900">Ghi nhận yêu cầu</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <label className="text-xs font-medium text-slate-700">
          Học viên
          <select
            value={studentId}
            onChange={(e) => setStudentId(e.target.value)}
            className="mt-1 w-full h-10 px-3 text-sm font-normal border rounded-lg bg-white"
          >
            <option value="">Chọn học viên</option>
            {students.map((s) => (
              <option key={s.id} value={s.id}>
                {s.fullName}
              </option>
            ))}
          </select>
        </label>
        <label className="text-xs font-medium text-slate-700">
          Người yêu cầu
          <Input
            value={requesterName}
            maxLength={MAX_REQUESTER}
            onChange={(e) => setRequesterName(e.target.value)}
            className="mt-1 font-normal"
          />
        </label>
        <label className="text-xs font-medium text-slate-700">
          Loại yêu cầu
          <select
            value={type}
            onChange={(e) => setType(e.target.value as DataRequestType)}
            className="mt-1 w-full h-10 px-3 text-sm font-normal border rounded-lg bg-white"
          >
            <option value="ACCESS">{TYPE_LABEL.ACCESS}</option>
            <option value="ERASURE">{TYPE_LABEL.ERASURE}</option>
          </select>
        </label>
        <label className="text-xs font-medium text-slate-700">
          Ngày nhận
          <Input
            type="date"
            value={receivedOn}
            max={maxDate}
            onChange={(e) => setReceivedOn(e.target.value)}
            className="mt-1 font-normal"
          />
        </label>
      </div>
      <div className="flex justify-end">
        <Button
          type="button"
          size="sm"
          disabled={!studentId || !requesterName.trim() || !receivedOn || createMutation.isPending}
          onClick={() => createMutation.mutate()}
        >
          {createMutation.isPending ? 'Đang lưu…' : 'Ghi nhận'}
        </Button>
      </div>
    </section>
  )
}

function RequestActions({
  request,
  studentName,
}: {
  request: DataRequestItem
  studentName: string
}) {
  const queryClient = useQueryClient()
  const showError = useApiErrorToast()
  const [rejecting, setRejecting] = useState(false)
  const [note, setNote] = useState('')
  const [confirmAnonymize, setConfirmAnonymize] = useState(false)

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['data-subject-requests'] })

  const completeMutation = useMutation({
    mutationFn: () => completeAccessRequest(request.id),
    onSuccess: () => {
      refresh()
      toast.success('Đã hoàn tất yêu cầu xem dữ liệu.')
    },
    onError: (err) => showError(err),
  })
  const rejectMutation = useMutation({
    mutationFn: () => rejectDataRequest(request.id, note.trim()),
    onSuccess: () => {
      refresh()
      toast.success('Đã từ chối yêu cầu.')
    },
    onError: (err) => showError(err),
  })
  const anonymizeMutation = useMutation({
    mutationFn: () => anonymizeStudent(request.id),
    onSuccess: () => {
      setConfirmAnonymize(false)
      // Tên, trạng thái và buổi đã đặt của học viên đều đổi
      queryClient.invalidateQueries()
      toast.success('Đã ẩn danh hóa học viên.')
    },
    onError: (err) => {
      setConfirmAnonymize(false)
      showError(err)
    },
  })

  if (request.status !== 'RECEIVED') return null

  if (rejecting) {
    return (
      <div className="space-y-2 min-w-56">
        <label className="block text-xs font-medium text-slate-700 text-left">
          Lý do từ chối
          <textarea
            rows={2}
            maxLength={MAX_NOTE}
            value={note}
            onChange={(e) => setNote(e.target.value)}
            className="mt-1 w-full px-3 py-2 text-sm font-normal border rounded-lg bg-white"
          />
        </label>
        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" size="sm" onClick={() => setRejecting(false)}>
            Hủy
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={!note.trim() || rejectMutation.isPending}
            onClick={() => rejectMutation.mutate()}
          >
            Xác nhận từ chối
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-wrap justify-end gap-2">
      {request.type === 'ACCESS' ? (
        <>
          <Link
            to={`/admin/privacy/students/${request.studentId}`}
            className="inline-flex items-center h-8 px-3 text-sm font-medium rounded-[var(--radius)] border border-border bg-white hover:bg-muted"
          >
            Mở bản tóm tắt
          </Link>
          <Button
            type="button"
            size="sm"
            disabled={completeMutation.isPending}
            onClick={() => completeMutation.mutate()}
          >
            Hoàn tất
          </Button>
        </>
      ) : (
        <Button
          type="button"
          variant="destructive"
          size="sm"
          onClick={() => setConfirmAnonymize(true)}
        >
          Ẩn danh hóa
        </Button>
      )}
      <Button type="button" variant="ghost" size="sm" onClick={() => setRejecting(true)}>
        Từ chối
      </Button>

      <ConfirmDialog
        open={confirmAnonymize}
        title={`Ẩn danh hóa ${studentName}?`}
        description={
          <>
            <strong>Thao tác này không khôi phục được.</strong> Họ tên, liên hệ, ngày sinh, hồ sơ
            năng lực, lộ trình, bài tập và nhận xét sẽ bị xóa; tài khoản bị ngừng vĩnh viễn; các
            buổi đã đặt chuyển sang hủy hệ thống. Số liệu tài chính và số buổi đã dạy được giữ
            nguyên.
          </>
        }
        confirmLabel="Ẩn danh hóa vĩnh viễn"
        destructive
        busy={anonymizeMutation.isPending}
        onConfirm={() => anonymizeMutation.mutate()}
        onCancel={() => setConfirmAnonymize(false)}
      />
    </div>
  )
}

export default function DataSubjectRequestsPage() {
  const {
    data: requests = [],
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['data-subject-requests'],
    queryFn: fetchDataRequests,
  })
  const { data: users = [] } = useQuery({
    queryKey: ['users', 'students-for-privacy'],
    queryFn: () => fetchUsers({ role: 'STUDENT' }),
  })

  const nameOf = new Map(
    users.map((u) => [u.id, u.anonymized ? 'Học viên đã ẩn danh' : u.fullName]),
  )
  const selectable = users.filter((u) => !u.anonymized)

  return (
    <div className="space-y-6 max-w-5xl mx-auto p-4 sm:p-6">
      <div>
        <h1 className="text-2xl tracking-tight text-slate-900">Dữ liệu cá nhân</h1>
        <p className="text-sm text-slate-500 mt-1">
          Ghi nhận và xử lý yêu cầu xem hoặc xóa dữ liệu của học viên.
        </p>
      </div>

      <CreateRequestForm students={selectable} />

      <section className="bg-white rounded-xl border overflow-hidden">
        <h2 className="text-base text-slate-900 p-5 pb-3">Các yêu cầu</h2>
        {isLoading ? (
          <p className="px-5 pb-5 text-sm text-slate-400">Đang tải…</p>
        ) : isError ? (
          <p role="alert" className="px-5 pb-5 text-sm text-rose-600">
            Không tải được danh sách yêu cầu. Vui lòng thử lại.
          </p>
        ) : requests.length === 0 ? (
          <p className="px-5 pb-5 text-sm text-slate-500">Chưa có yêu cầu nào.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-y text-xs font-semibold text-slate-500">
                <tr>
                  <th className="py-2.5 px-5">Học viên</th>
                  <th className="py-2.5 px-3">Người yêu cầu</th>
                  <th className="py-2.5 px-3">Loại</th>
                  <th className="py-2.5 px-3">Ngày nhận</th>
                  <th className="py-2.5 px-3">Kết quả</th>
                  <th className="py-2.5 px-5 text-right">Xử lý</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {requests.map((r) => {
                  const name = nameOf.get(r.studentId) ?? 'Không rõ tên'
                  return (
                    <tr key={r.id} className="align-top">
                      <td className="py-3 px-5 text-slate-900">{name}</td>
                      <td className="py-3 px-3">{r.requesterName}</td>
                      <td className="py-3 px-3 whitespace-nowrap">{TYPE_LABEL[r.type]}</td>
                      <td className="py-3 px-3 whitespace-nowrap">
                        <DateTimeText value={`${r.receivedOn}T12:00:00Z`} mode="date" />
                      </td>
                      <td className="py-3 px-3">
                        <StatusBadge status={r.status === 'COMPLETED' ? 'DONE' : r.status} />
                        {r.resolvedAt && (
                          <p className="text-xs text-slate-500 mt-1 whitespace-nowrap">
                            <DateTimeText value={r.resolvedAt} />
                          </p>
                        )}
                        {r.resolutionNote && (
                          <p className="text-xs text-slate-600 mt-1">{r.resolutionNote}</p>
                        )}
                      </td>
                      <td className="py-3 px-5">
                        <RequestActions request={r} studentName={name} />
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
