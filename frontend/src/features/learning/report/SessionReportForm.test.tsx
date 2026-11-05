import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { SessionReportDto } from '@/lib/api/learning'
import SessionReportForm from './SessionReportForm'

vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'tch-1', role: 'TEACHER', timezone: 'Asia/Ho_Chi_Minh' } }),
}))

const report: SessionReportDto = {
  id: 'rep-1',
  sessionId: 'ses-1',
  teacherId: 'tch-1',
  studentId: 'stu-1',
  status: 'PENDING',
  sessionEndsAt: '2026-11-05T10:00:00Z',
  dueAt: '2026-11-06T10:00:00Z',
  overdue: false,
  taughtTopicIds: [],
}

const path = {
  id: 'path-1',
  studentId: 'stu-1',
  title: 'Lộ trình',
  status: 'ACTIVE',
  completionPercent: 0,
  milestones: [
    {
      id: 'm-1',
      position: 0,
      title: 'Chặng 1',
      topics: [
        { id: 't-1', position: 0, title: 'Thì hiện tại', status: 'PLANNED' },
        { id: 't-2', position: 1, title: 'Giới thiệu bản thân', status: 'PLANNED' },
      ],
    },
  ],
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function mockApi(pathResponse: () => Response = () => json(200, path)) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input)
    if (url.endsWith('/learning-path')) return pathResponse()
    if (url.endsWith('/sessions/ses-1/report') && init?.method === 'PUT') {
      return json(200, { ...report, status: 'SUBMITTED' })
    }
    return json(200, [])
  })
}

function renderForm() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <SessionReportForm report={report} />
    </QueryClientProvider>,
  )
}

function submittedBody(fetchMock: ReturnType<typeof mockApi>) {
  const call = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT')
  return call ? JSON.parse(String(call[1]!.body)) : undefined
}

describe('SessionReportForm', () => {
  beforeEach(() => {
    // Dialog gốc của jsdom chưa có showModal
    HTMLDialogElement.prototype.showModal = function () {
      this.setAttribute('open', '')
    }
    HTMLDialogElement.prototype.close = function () {
      this.removeAttribute('open')
    }
    localStorage.clear()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('không nộp khi chưa chấm kỹ năng và chưa có nhận xét', async () => {
    const fetchMock = mockApi()
    renderForm()

    await userEvent.click(screen.getByRole('button', { name: 'Nộp phiếu' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Cần chấm ít nhất một kỹ năng.')
    expect(screen.getByRole('alert')).toHaveTextContent('Cần nhận xét buổi học.')
    expect(submittedBody(fetchMock)).toBeUndefined()
  })

  it('nộp đúng dữ liệu: chủ đề, điểm bỏ trống được, nhận xét, bài tập', async () => {
    const fetchMock = mockApi()
    renderForm()
    const user = userEvent.setup()

    await user.click(await screen.findByLabelText('Đã dạy: Thì hiện tại'))
    await user.click(screen.getByLabelText('Hoàn thành: Giới thiệu bản thân'))
    await user.click(
      screen.getByRole('group', { name: 'Nghe' }).querySelectorAll('button')[3] as HTMLElement,
    )
    await user.type(screen.getByLabelText('Nhận xét'), '  Tiến bộ tốt  ')
    await user.click(screen.getByRole('button', { name: 'Thêm bài tập' }))
    await user.type(screen.getByLabelText('Tiêu đề bài tập 1'), 'Viết đoạn văn')
    await user.click(screen.getByRole('button', { name: 'Nộp phiếu' }))
    await user.click(screen.getAllByRole('button', { name: 'Nộp phiếu', hidden: true })[1])

    await waitFor(() => expect(submittedBody(fetchMock)).toBeDefined())
    expect(submittedBody(fetchMock)).toEqual({
      taughtTopicIds: ['t-1'],
      completedTopicIds: ['t-2'],
      scores: { listening: 4 },
      comment: 'Tiến bộ tốt',
      assignments: [{ title: 'Viết đoạn văn' }],
    })
    await waitFor(() => expect(localStorage.getItem('session-report-draft:ses-1')).toBeNull())
  })

  it('bấm lại điểm thì bỏ trống kỹ năng đó', async () => {
    mockApi()
    renderForm()
    const user = userEvent.setup()
    const five = screen.getByRole('group', { name: 'Nói' }).querySelectorAll('button')[4]

    await user.click(five)
    expect(five).toHaveAttribute('aria-pressed', 'true')
    await user.click(five)
    expect(five).toHaveAttribute('aria-pressed', 'false')
  })

  it('khôi phục nháp sau khi tải lại', async () => {
    mockApi()
    const first = renderForm()
    await userEvent.type(screen.getByLabelText('Nhận xét'), 'Nháp dở')
    first.unmount()

    renderForm()
    expect(screen.getByLabelText('Nhận xét')).toHaveValue('Nháp dở')
  })

  it('không có lộ trình vẫn nộp được phiếu', async () => {
    mockApi(() => json(404, { code: 'NOT_FOUND', message: 'x' }))
    renderForm()

    expect(await screen.findByText(/Không có chủ đề để chọn/)).toBeInTheDocument()
  })
})
