import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { SessionReportDto } from '@/lib/api/learning'
import SessionReportSection from './SessionReportSection'

const auth = { role: 'ADMIN' }
vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'adm-1', role: auth.role, timezone: 'Asia/Ho_Chi_Minh' } }),
}))

const report: SessionReportDto = {
  id: 'rep-1',
  sessionId: 'ses-1',
  teacherId: 'tch-1',
  studentId: 'stu-1',
  status: 'PENDING',
  sessionEndsAt: '2026-11-05T10:00:00Z',
  dueAt: '2026-11-06T10:00:00Z',
  overdue: true,
  taughtTopicIds: [],
  teacherAssigned: false,
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function mockApi(current: SessionReportDto) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    if (String(input).endsWith('/report/close') && init?.method === 'POST') {
      return json(200, { ...current, status: 'CLOSED' })
    }
    return json(200, current)
  })
}

function renderSection() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <SessionReportSection sessionId="ses-1" />
    </QueryClientProvider>,
  )
}

function closeBody(fetchMock: ReturnType<typeof mockApi>) {
  const call = fetchMock.mock.calls.find(([url]) => String(url).endsWith('/report/close'))
  return call ? JSON.parse(String(call[1]!.body)) : undefined
}

describe('SessionReportSection (T148)', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    auth.role = 'ADMIN'
  })

  it('giáo vụ đóng phiếu của giáo viên không còn phụ trách, kèm lý do', async () => {
    const fetchMock = mockApi(report)
    renderSection()
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Đóng phiếu' }))
    const confirm = screen.getByRole('button', { name: 'Xác nhận đóng phiếu' })
    expect(confirm).toBeDisabled()
    await user.type(screen.getByLabelText('Lý do đóng phiếu'), '  Giáo viên nghỉ việc  ')
    await user.click(confirm)

    await waitFor(() => expect(closeBody(fetchMock)).toEqual({ reason: 'Giáo viên nghỉ việc' }))
  })

  it('giáo viên còn phụ trách thì giáo vụ không đóng được', async () => {
    mockApi({ ...report, teacherAssigned: true })
    renderSection()

    expect(await screen.findByText(/Phiếu đánh giá chưa được nộp/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Đóng phiếu' })).not.toBeInTheDocument()
  })

  it('học viên không thấy nút đóng phiếu', async () => {
    auth.role = 'STUDENT'
    mockApi(report)
    renderSection()

    expect(await screen.findByText(/Phiếu đánh giá chưa được nộp/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Đóng phiếu' })).not.toBeInTheDocument()
  })

  it('phiếu đã đóng hiện lý do', async () => {
    mockApi({
      ...report,
      status: 'CLOSED',
      closeReason: 'Giáo viên nghỉ việc',
      closedAt: '2026-11-08T03:00:00Z',
    })
    renderSection()

    expect(await screen.findByText(/đã được giáo vụ đóng/)).toHaveTextContent(
      'Lý do: Giáo viên nghỉ việc',
    )
  })
})
