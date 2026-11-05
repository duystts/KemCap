import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import type { AssignmentItem } from '@/lib/api/learning'
import AssignmentsPage from './AssignmentsPage'

const auth = vi.hoisted(() => ({ role: 'STUDENT' }))

vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'me-1', role: auth.role, timezone: 'Asia/Ho_Chi_Minh' } }),
}))

const base: AssignmentItem = {
  id: 'a-1',
  sessionId: 'ses-1',
  studentId: 'stu-1',
  teacherId: 'tch-1',
  title: 'Viết đoạn văn',
  instructions: 'Dùng thì quá khứ',
  dueAt: '2099-01-01T00:00:00Z',
  status: 'PENDING',
  submission: null,
}
const submission = {
  textAnswer: 'Bài của em',
  submittedAt: '2026-11-05T10:00:00Z',
  late: false,
  score: null,
  feedback: null,
  gradedBy: null,
  gradedAt: null,
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function mockApi(assignments: AssignmentItem[], grade?: () => Response) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = String(input)
    if (url.endsWith('/submission')) return json(200, base)
    if (url.endsWith('/grade')) return grade?.() ?? json(200, base)
    if (url.includes('/students')) return json(200, [{ id: 'stu-1', fullName: 'Lê Hoàng An' }])
    return json(200, assignments)
  })
}

function renderPage(role: string) {
  auth.role = role
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <AssignmentsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function requestBody(fetchMock: ReturnType<typeof mockApi>, path: string) {
  const call = fetchMock.mock.calls.find(([url]) => String(url).endsWith(path))
  return call ? JSON.parse(String(call[1]!.body)) : undefined
}

describe('AssignmentsPage', () => {
  beforeEach(() => {
    HTMLDialogElement.prototype.showModal = function () {
      this.setAttribute('open', '')
    }
    HTMLDialogElement.prototype.close = function () {
      this.removeAttribute('open')
    }
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('học viên nộp bài: bỏ khoảng trắng thừa, chưa có nội dung thì không nộp được', async () => {
    const fetchMock = mockApi([base])
    renderPage('STUDENT')
    const user = userEvent.setup()

    const button = await screen.findByRole('button', { name: 'Nộp bài' })
    expect(button).toBeDisabled()
    await user.type(screen.getByLabelText('Bài làm'), '  Bài của em  ')
    await user.click(button)

    await waitFor(() =>
      expect(requestBody(fetchMock, '/a-1/submission')).toEqual({ textAnswer: 'Bài của em' }),
    )
  })

  it('học viên thấy cảnh báo khi đã quá hạn và nhãn Nộp muộn', async () => {
    mockApi([
      { ...base, dueAt: '2020-01-01T00:00:00Z' },
      {
        ...base,
        id: 'a-2',
        title: 'Bài muộn',
        status: 'SUBMITTED',
        submission: { ...submission, late: true },
      },
    ])
    renderPage('STUDENT')

    expect(await screen.findByText(/bài nộp bây giờ sẽ ghi là nộp muộn/)).toBeInTheDocument()
    expect(screen.getByText('Quá hạn')).toBeInTheDocument()
    expect(screen.getByText('Nộp muộn')).toBeInTheDocument()
  })

  it('bài đã chấm chỉ đọc, hiện điểm 0 và phản hồi', async () => {
    mockApi([
      {
        ...base,
        status: 'GRADED',
        submission: { ...submission, score: 0, feedback: 'Cần làm lại' },
      },
    ])
    renderPage('STUDENT')

    expect(await screen.findByText('Điểm: 0/10')).toBeInTheDocument()
    expect(screen.getByText('Cần làm lại')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Sửa bài|Nộp bài/ })).not.toBeInTheDocument()
  })

  it('giáo viên chấm bài sau khi xác nhận, điểm ngoài 0–10 thì không chấm được', async () => {
    const fetchMock = mockApi([{ ...base, status: 'SUBMITTED', submission }])
    renderPage('TEACHER')
    const user = userEvent.setup()

    expect(await screen.findByText(/Lê Hoàng An/)).toBeInTheDocument()
    const score = screen.getByLabelText('Điểm (0–10)')
    await user.type(score, '11')
    expect(screen.getByRole('button', { name: 'Chấm bài' })).toBeDisabled()
    await user.clear(score)
    await user.type(score, '8')
    await user.type(screen.getByLabelText('Phản hồi (không bắt buộc)'), 'Tốt')
    await user.click(screen.getByRole('button', { name: 'Chấm bài' }))
    const dialog = screen.getByRole('dialog', { hidden: true })
    await user.click(within(dialog).getByRole('button', { name: 'Chấm bài', hidden: true }))

    await waitFor(() =>
      expect(requestBody(fetchMock, '/a-1/grade')).toEqual({ score: 8, feedback: 'Tốt' }),
    )
  })

  it('giáo viên không chấm được bài chưa nộp, học viên không có form chấm', async () => {
    mockApi([base])
    const first = renderPage('TEACHER')
    await screen.findByText('Viết đoạn văn')
    expect(screen.queryByLabelText('Điểm (0–10)')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Nộp bài' })).not.toBeInTheDocument()
    first.unmount()

    mockApi([{ ...base, status: 'SUBMITTED', submission }])
    renderPage('STUDENT')
    await screen.findByText('Bài của em')
    expect(screen.queryByLabelText('Điểm (0–10)')).not.toBeInTheDocument()
  })

  it('lọc theo trạng thái', async () => {
    mockApi([base, { ...base, id: 'a-2', title: 'Bài đã nộp', status: 'SUBMITTED', submission }])
    renderPage('STUDENT')
    const user = userEvent.setup()

    await screen.findByText('Viết đoạn văn')
    await user.click(screen.getByRole('button', { name: 'Đã nộp (1)' }))

    expect(screen.getByText('Bài đã nộp')).toBeInTheDocument()
    expect(screen.queryByText('Viết đoạn văn')).not.toBeInTheDocument()
  })
})
