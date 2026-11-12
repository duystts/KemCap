import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import type { SessionItem } from '@/lib/api/scheduling'
import SessionFeedbackPrompt from './SessionFeedbackPrompt'
import StudentHomePage from './StudentHomePage'

vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'stu-1', role: 'STUDENT', timezone: 'Asia/Ho_Chi_Minh' } }),
}))

const hoursAgo = (h: number) => new Date(Date.now() - h * 3600_000).toISOString()

function session(over: Partial<SessionItem> = {}): SessionItem {
  return {
    id: 'ses-1',
    enrollmentId: 'enr-1',
    teacherId: 'tch-1',
    studentId: 'stu-1',
    startsAt: hoursAgo(3),
    endsAt: hoursAgo(2),
    status: 'COMPLETED',
    version: 0,
    ...over,
  }
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function mockApi(options: {
  mine?: { sessionId: string; stars: number; comment?: string; createdAt?: string }[]
  sessions?: SessionItem[]
  booked?: SessionItem[]
  enrollments?: unknown[]
  post?: () => Response
}) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input)
    if (url.includes('/session-feedback/mine')) return json(200, options.mine ?? [])
    if (init?.method === 'POST') return options.post?.() ?? json(201, {})
    if (url.includes('status=BOOKED')) return json(200, options.booked ?? [])
    if (url.includes('/enrollments')) return json(200, options.enrollments ?? [])
    return json(200, options.sessions ?? [])
  })
}

function wrap(node: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>{node}</MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('SessionFeedbackPrompt', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('chưa chọn sao thì không gửi được; gửi đúng số sao và nhận xét', async () => {
    const fetchMock = mockApi({})
    wrap(<SessionFeedbackPrompt session={session()} />)
    const user = userEvent.setup()

    const send = await screen.findByRole('button', { name: 'Gửi đánh giá' })
    expect(send).toBeDisabled()
    await user.click(screen.getByRole('button', { name: '4 sao' }))
    expect(screen.getByRole('button', { name: '1 sao' })).toHaveClass('text-indigo-600')
    expect(screen.getByRole('button', { name: '4 sao' })).toHaveClass('text-indigo-600')
    expect(screen.getByRole('button', { name: '5 sao' })).toHaveClass('text-slate-300')
    await user.type(screen.getByLabelText('Nhận xét (không bắt buộc)'), ' Rất tốt ')
    await user.click(send)

    await waitFor(() => {
      const call = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST')
      expect(String(call?.[0])).toContain('/sessions/ses-1/feedback')
      expect(JSON.parse(String(call?.[1]?.body))).toEqual({ stars: 4, comment: 'Rất tốt' })
    })
  })

  it('đã gửi: hiện lại đánh giá của mình, không còn form', async () => {
    mockApi({
      mine: [{ sessionId: 'ses-1', stars: 4, comment: 'Cô dạy dễ hiểu', createdAt: hoursAgo(1) }],
    })
    wrap(<SessionFeedbackPrompt session={session()} />)

    expect(await screen.findByText('Đánh giá của bạn')).toBeInTheDocument()
    expect(screen.getByLabelText('4 sao')).toBeInTheDocument()
    expect(screen.getByText('Cô dạy dễ hiểu')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Gửi đánh giá' })).not.toBeInTheDocument()
  })

  it('đã gửi và đã quá 7 ngày: vẫn xem lại được', async () => {
    mockApi({ mine: [{ sessionId: 'ses-1', stars: 5, createdAt: hoursAgo(24 * 8) }] })
    wrap(<SessionFeedbackPrompt session={session({ endsAt: hoursAgo(24 * 9) })} />)

    expect(await screen.findByText('Đánh giá của bạn')).toBeInTheDocument()
    expect(screen.getByLabelText('5 sao')).toBeInTheDocument()
  })

  it('ẩn khi quá 7 ngày hoặc buổi chưa hoàn thành', async () => {
    mockApi({})
    const late = wrap(<SessionFeedbackPrompt session={session({ endsAt: hoursAgo(24 * 7 + 1) })} />)
    await new Promise((r) => setTimeout(r, 50))
    expect(late.container).toBeEmptyDOMElement()
    late.unmount()

    const booked = wrap(<SessionFeedbackPrompt session={session({ status: 'BOOKED' })} />)
    await new Promise((r) => setTimeout(r, 50))
    expect(booked.container).toBeEmptyDOMElement()
  })

  it('hết hạn lúc gửi thì ẩn form', async () => {
    mockApi({ post: () => json(409, { code: 'FEEDBACK_WINDOW_CLOSED', message: 'x' }) })
    wrap(<SessionFeedbackPrompt session={session()} />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: '5 sao' }))
    await user.click(screen.getByRole('button', { name: 'Gửi đánh giá' }))

    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Gửi đánh giá' })).not.toBeInTheDocument(),
    )
  })
})

describe('StudentHomePage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('chỉ mời đánh giá buổi còn hạn và chưa chấm', async () => {
    mockApi({
      sessions: [
        session(),
        session({ id: 'ses-2' }),
        session({ id: 'ses-3', endsAt: hoursAgo(24 * 8) }),
      ],
      mine: [{ sessionId: 'ses-2', stars: 3 }],
    })
    wrap(<StudentHomePage />)

    expect(await screen.findAllByRole('button', { name: 'Gửi đánh giá' })).toHaveLength(1)
  })

  it('hiện buổi sắp tới gần nhất trước, bỏ buổi đã qua; cộng số buổi còn lại của gói đang dùng', async () => {
    const hoursAhead = (h: number) => new Date(Date.now() + h * 3600_000).toISOString()
    mockApi({
      booked: [
        session({
          id: 'later',
          startsAt: hoursAhead(48),
          endsAt: hoursAhead(49),
          status: 'BOOKED',
        }),
        session({ id: 'sooner', startsAt: hoursAhead(2), endsAt: hoursAhead(3), status: 'BOOKED' }),
        session({ id: 'past', startsAt: hoursAgo(5), endsAt: hoursAgo(4), status: 'BOOKED' }),
      ],
      enrollments: [
        { id: 'e-1', status: 'ACTIVE', sessionsRemaining: 5 },
        { id: 'e-2', status: 'ACTIVE', sessionsRemaining: 2 },
        { id: 'e-3', status: 'EXPIRED', sessionsRemaining: 9 },
      ],
    })
    wrap(<StudentHomePage />)

    const links = await screen.findAllByRole('link', { name: /Xem chi tiết/ })
    expect(links.map((l) => l.getAttribute('href'))).toEqual([
      '/sessions/sooner',
      '/sessions/later',
    ])
    expect(await screen.findByText('7')).toBeInTheDocument()
  })

  it('chưa có buổi và chưa có gói: có hướng dẫn', async () => {
    mockApi({})
    wrap(<StudentHomePage />)

    expect(await screen.findByRole('link', { name: 'Đặt lịch' })).toHaveAttribute(
      'href',
      '/student/book',
    )
    expect(await screen.findByText('Chưa có gói đang dùng.')).toBeInTheDocument()
  })

  it('không có buổi cần đánh giá', async () => {
    mockApi({})
    wrap(<StudentHomePage />)

    expect(await screen.findByText('Không có buổi học nào cần đánh giá.')).toBeInTheDocument()
  })
})
