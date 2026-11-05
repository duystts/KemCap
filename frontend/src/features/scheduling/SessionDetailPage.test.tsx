import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes, useParams } from 'react-router'
import SessionDetailPage from './SessionDetailPage'

const auth = vi.hoisted(() => ({ role: 'STUDENT' }))

vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'me-1', role: auth.role } }),
}))

const NOW = Date.now()
const session = {
  id: 'ses-1',
  enrollmentId: 'enr-1',
  teacherId: 'tch-1',
  studentId: 'stu-1',
  startsAt: new Date(NOW + 48 * 3600_000).toISOString(),
  endsAt: new Date(NOW + 49 * 3600_000).toISOString(),
  status: 'BOOKED',
  version: 0,
}
const slot = {
  startsAt: new Date(NOW + 72 * 3600_000).toISOString(),
  endsAt: new Date(NOW + 73 * 3600_000).toISOString(),
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function mockApi(options: {
  status?: string
  willConsume?: boolean
  reschedule?: () => Response
  decision?: () => Response
}) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = String(input)
    if (url.includes('/cancel-preview')) {
      return json(200, { willConsume: options.willConsume ?? false, cutoffAt: session.startsAt })
    }
    if (url.includes('/slots')) return json(200, [slot])
    if (url.endsWith('/reschedule')) {
      return options.reschedule?.() ?? json(200, { ...session, id: 'ses-2' })
    }
    if (url.endsWith('/interruption-decision')) {
      return options.decision?.() ?? json(200, session)
    }
    return json(200, { ...session, status: options.status ?? 'BOOKED' })
  })
}

function SessionRoute() {
  const { id } = useParams()
  return id === 'ses-1' ? <SessionDetailPage /> : <p>Trang buổi {id}</p>
}

function renderPage(role: string) {
  auth.role = role
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/sessions/ses-1']}>
        <Routes>
          <Route path="/sessions/:id" element={<SessionRoute />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function requestBody(fetchMock: ReturnType<typeof mockApi>, path: string) {
  const call = fetchMock.mock.calls.find(([url]) => String(url).endsWith(path))
  return call ? JSON.parse(String(call[1]!.body)) : undefined
}

describe('SessionDetailPage', () => {
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

  it('học viên đổi lịch: gửi giờ mới rồi sang trang buổi mới', async () => {
    const fetchMock = mockApi({})
    renderPage('STUDENT')
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Đổi lịch' }))
    expect(screen.getByRole('button', { name: 'Xác nhận đổi lịch' })).toBeDisabled()
    await user.click(await screen.findByTestId('slot-item'))
    await user.click(screen.getByRole('button', { name: 'Xác nhận đổi lịch' }))

    expect(await screen.findByText('Trang buổi ses-2')).toBeInTheDocument()
    expect(requestBody(fetchMock, '/reschedule')).toEqual({ newStartsAt: slot.startsAt })
  })

  it('khung giờ vừa bị đặt: giữ form và bỏ chọn', async () => {
    mockApi({ reschedule: () => json(409, { code: 'SLOT_TAKEN', message: 'x' }) })
    renderPage('STUDENT')
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Đổi lịch' }))
    await user.click(await screen.findByTestId('slot-item'))
    await user.click(screen.getByRole('button', { name: 'Xác nhận đổi lịch' }))

    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Xác nhận đổi lịch' })).toBeDisabled(),
    )
  })

  it('quá hạn chót: ẩn đổi lịch, vẫn còn nút hủy', async () => {
    mockApi({ willConsume: true })
    renderPage('STUDENT')

    expect(await screen.findByText(/Đã quá hạn đổi lịch/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Đổi lịch' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Hủy lịch buổi học/ })).toBeInTheDocument()
  })

  it('giáo viên không đổi lịch và không gọi cancel-preview', async () => {
    const fetchMock = mockApi({})
    renderPage('TEACHER')

    await screen.findByRole('button', { name: /Hủy lịch buổi học/ })
    expect(screen.queryByRole('button', { name: 'Đổi lịch' })).not.toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('cancel-preview'))).toBe(false)
  })

  it('giáo vụ quyết định gián đoạn: tính là đã học', async () => {
    const fetchMock = mockApi({ status: 'INTERRUPTED' })
    renderPage('ADMIN')
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Tính là đã học' }))
    const buttons = screen.getAllByRole('button', { name: 'Tính là đã học', hidden: true })
    await user.click(buttons[buttons.length - 1])

    await waitFor(() =>
      expect(requestBody(fetchMock, '/interruption-decision')).toEqual({ decision: 'COUNT' }),
    )
  })

  it('giáo vụ xếp bù: gửi MAKE_UP và ẩn nút trong phiên', async () => {
    const fetchMock = mockApi({ status: 'INTERRUPTED' })
    renderPage('ADMIN')
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Xếp bù' }))
    const buttons = screen.getAllByRole('button', { name: 'Xếp bù', hidden: true })
    await user.click(buttons[buttons.length - 1])

    expect(await screen.findByText(/Đã xếp bù: buổi này không bị trừ/)).toBeInTheDocument()
    expect(requestBody(fetchMock, '/interruption-decision')).toEqual({ decision: 'MAKE_UP' })
  })

  it('giáo viên không thấy quyết định gián đoạn', async () => {
    mockApi({ status: 'INTERRUPTED' })
    renderPage('TEACHER')

    await screen.findByText('Chi tiết Buổi học 1-1')
    expect(screen.queryByRole('button', { name: 'Tính là đã học' })).not.toBeInTheDocument()
  })
})
