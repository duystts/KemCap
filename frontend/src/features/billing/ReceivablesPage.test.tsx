import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import ReceivablesPage from './ReceivablesPage'

const debtor = {
  enrollmentId: 'enr-1',
  studentId: 'stu-1',
  studentName: 'An Nguyễn',
  amountDueVnd: 3000000,
  amountPaidVnd: 1000000,
  debtVnd: 2000000,
  cancelled: false,
}
const otherDebtor = {
  ...debtor,
  enrollmentId: 'enr-2',
  studentId: 'stu-2',
  studentName: 'Bình Lê',
  debtVnd: 500000,
}
const cancelled = {
  ...debtor,
  enrollmentId: 'enr-3',
  studentId: 'stu-3',
  studentName: 'Chi Trần',
  cancelled: true,
}
const overpaid = {
  ...debtor,
  enrollmentId: 'enr-4',
  studentId: 'stu-4',
  studentName: 'Dũng Phạm',
  debtVnd: -100000,
}

function mockReceivables() {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const url = String(input)
    const body = url.includes('hasDebt=true')
      ? [debtor, otherDebtor]
      : [debtor, otherDebtor, cancelled, overpaid]
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
        <ReceivablesPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ReceivablesPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('mặc định chỉ lấy học viên còn nợ, có link tới ghi danh', async () => {
    const fetchMock = mockReceivables()
    renderPage()

    const row = (await screen.findByText('An Nguyễn')).closest('tr')!
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/receivables?hasDebt=true'),
      expect.any(Object),
    )
    expect(within(row).getByRole('link', { name: /Mở ghi danh/ })).toHaveAttribute(
      'href',
      '/admin/enrollments/enr-1',
    )
  })

  it('tổng công nợ không đổi khi hiện cả ghi danh đã hủy và thu vượt', async () => {
    mockReceivables()
    renderPage()
    await screen.findByText('An Nguyễn')
    const total = (2500000).toLocaleString('vi-VN') + ' đ'
    expect(screen.getByText(total)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('checkbox'))

    expect(await screen.findByText('Chi Trần')).toBeInTheDocument()
    expect(screen.getByText('Đã hủy')).toBeInTheDocument()
    expect(screen.getByText(total)).toBeInTheDocument()
  })

  it('lọc theo tên học viên', async () => {
    mockReceivables()
    renderPage()
    await screen.findByText('An Nguyễn')

    await userEvent.type(screen.getByPlaceholderText('Tìm theo tên học viên…'), 'bình')

    expect(screen.getByText('Bình Lê')).toBeInTheDocument()
    expect(screen.queryByText('An Nguyễn')).not.toBeInTheDocument()
  })
})
