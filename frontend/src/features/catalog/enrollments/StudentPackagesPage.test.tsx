import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import StudentPackagesPage from './StudentPackagesPage'

vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'stu-1', role: 'STUDENT' } }),
}))

const days = (n: number) => new Date(Date.now() + n * 24 * 3600 * 1000).toISOString()
const vnd = (n: number) => n.toLocaleString('vi-VN') + ' đ'

const base = {
  id: 'e-1',
  studentId: 'stu-1',
  teacherId: 't-1',
  packageId: 'p-1',
  sessionsTotal: 12,
  sessionsReserved: 1,
  sessionsConsumed: 3,
  sessionsRemaining: 8,
  sessionMinutes: 45,
  activatedAt: days(-10),
  expiresAt: days(60),
  status: 'ACTIVE',
  version: 0,
}

const statement = {
  enrollmentId: 'e-1',
  amountDueVnd: 3000000,
  amountPaidVnd: 1000000,
  debtVnd: 2000000,
  payments: [
    {
      id: 'p-1',
      amountVnd: 1000000,
      paidAt: '2026-11-02',
      method: 'BANK_TRANSFER',
      adjustment: false,
      recordedBy: 'a',
      recordedAt: '2026-11-02T10:00:00Z',
    },
    {
      id: 'p-2',
      amountVnd: -50000,
      paidAt: '2026-11-03',
      method: 'OTHER',
      adjustment: true,
      recordedBy: 'a',
      recordedAt: '2026-11-03T10:00:00Z',
    },
  ],
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function renderPage(enrollments: unknown[], payments: () => Response = () => json(200, statement)) {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) =>
    String(input).endsWith('/payments') ? payments() : json(200, enrollments),
  )
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <StudentPackagesPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('StudentPackagesPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('hiện công nợ và lịch sử thanh toán chỉ đọc, kể cả điều chỉnh', async () => {
    renderPage([base])

    expect(await screen.findByText(vnd(2000000))).toBeInTheDocument()
    const table = screen.getByRole('table', { name: 'Lịch sử thanh toán' })
    expect(within(table).getByText('02/11/2026')).toBeInTheDocument()
    expect(within(table).getByText('Chuyển khoản')).toBeInTheDocument()
    expect(within(table).getByText('Điều chỉnh')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /thu|Ghi nhận/i })).not.toBeInTheDocument()
  })

  it('không có banner khi còn nhiều buổi và còn hạn', async () => {
    renderPage([base])

    await screen.findByText(vnd(2000000))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it.each([
    [2, 60, /còn 2 buổi/],
    [8, 5, /còn 5 ngày sử dụng/],
  ])('banner gia hạn: còn %i buổi, %i ngày', async (remaining, day, text) => {
    renderPage([{ ...base, sessionsRemaining: remaining, expiresAt: days(day) }])

    expect(await screen.findByRole('status')).toHaveTextContent(text)
  })

  it('đúng ngưỡng: 3 buổi và 8 ngày thì chưa nhắc', async () => {
    renderPage([{ ...base, sessionsRemaining: 3, expiresAt: days(8) }])

    await screen.findByText(vnd(2000000))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('gói đã hết hạn không hiện banner gia hạn', async () => {
    renderPage([{ ...base, status: 'EXPIRED', sessionsRemaining: 1, expiresAt: days(-2) }])

    await screen.findByText(vnd(2000000))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('chờ thanh toán: có giải thích, không có nút đặt lịch', async () => {
    renderPage([{ ...base, status: 'PENDING_PAYMENT', expiresAt: null, activatedAt: null }])

    expect(
      await screen.findByText(/sau khi giáo vụ ghi nhận thanh toán đầu tiên/),
    ).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Đặt lịch ngay' })).not.toBeInTheDocument()
  })

  it('lỗi tải học phí không làm mất thông tin gói', async () => {
    renderPage([base], () => json(500, { code: 'INTERNAL_ERROR', message: 'x' }))

    expect(await screen.findByText('Không tải được thông tin học phí.')).toBeInTheDocument()
    expect(screen.getByText('12 buổi (45 phút/buổi)')).toBeInTheDocument()
  })
})
