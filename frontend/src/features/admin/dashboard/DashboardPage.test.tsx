import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import DashboardPage from './DashboardPage'
import { last30Days, lastMonth, localDate, thisMonth } from './dateRange'

const vnd = (n: number) => n.toLocaleString('vi-VN') + ' đ'

function mockApi() {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = String(input)
    const body = url.includes('/reports/revenue')
      ? { collectedVnd: 5000000, debtVnd: 1200000 }
      : [
          { teacherId: 't-1', teacherName: 'Trần Văn Minh', sessionsTaught: 2 },
          { teacherId: 't-2', teacherName: 'Nguyễn Thị Lan', sessionsTaught: 7 },
        ]
    return new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('dateRange', () => {
  it('localDate lấy ngày theo giờ máy, kể cả lúc rạng sáng', () => {
    expect(localDate(new Date(2026, 10, 5, 1, 0))).toBe('2026-11-05')
  })

  it('tính đúng các khoảng, cả khi qua năm', () => {
    const today = new Date(2026, 0, 15)
    expect(thisMonth(today)).toEqual({ from: '2026-01-01', to: '2026-01-15' })
    expect(lastMonth(today)).toEqual({ from: '2025-12-01', to: '2025-12-31' })
    expect(last30Days(today)).toEqual({ from: '2025-12-17', to: '2026-01-15' })
  })
})

describe('DashboardPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('mặc định tháng này: gọi API đúng kỳ, hiện thẻ và bảng, giáo viên nhiều buổi đứng trước', async () => {
    const fetchMock = mockApi()
    renderPage()

    expect(await screen.findByText(vnd(5000000))).toBeInTheDocument()
    expect(screen.getByText(vnd(1200000))).toBeInTheDocument()
    const range = thisMonth(new Date())
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining(`/reports/revenue?from=${range.from}&to=${range.to}`),
      expect.any(Object),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining(`/reports/teacher-sessions?from=${range.from}&to=${range.to}`),
      expect.any(Object),
    )
    const rows = (await screen.findAllByRole('row')).slice(1)
    expect(within(rows[0]).getByText('Nguyễn Thị Lan')).toBeInTheDocument()
    expect(within(rows[2]).getByText('Tổng')).toBeInTheDocument()
    expect(within(rows[2]).getByText('9')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tháng này' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })

  it('đổi kỳ thì gọi lại API', async () => {
    const fetchMock = mockApi()
    renderPage()
    await screen.findByText(vnd(5000000))

    await userEvent.click(screen.getByRole('button', { name: 'Tháng trước' }))

    const range = lastMonth(new Date())
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining(`/reports/revenue?from=${range.from}&to=${range.to}`),
        expect.any(Object),
      ),
    )
  })

  it('từ ngày sau đến ngày: báo lỗi, không gọi API', async () => {
    const fetchMock = mockApi()
    renderPage()
    await screen.findByText(vnd(5000000))
    fetchMock.mockClear()

    fireEvent.change(screen.getByLabelText('Từ ngày'), { target: { value: '2099-01-01' } })

    expect(screen.getByRole('alert')).toHaveTextContent('Ngày bắt đầu phải trước ngày kết thúc.')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('xóa một ngày: nhắc chọn đủ', async () => {
    mockApi()
    renderPage()
    await screen.findByText(vnd(5000000))

    fireEvent.change(screen.getByLabelText('Đến ngày'), { target: { value: '' } })

    expect(screen.getByRole('alert')).toHaveTextContent('Hãy chọn đủ từ ngày và đến ngày.')
  })
})
