import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AdjustmentDialog from './AdjustmentDialog'

function renderDialog(onClose = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <AdjustmentDialog enrollmentId="enr-1" amountPaidVnd={1500000} onClose={onClose} />
    </QueryClientProvider>,
  )
  return onClose
}

afterEach(() => vi.restoreAllMocks())

describe('AdjustmentDialog', () => {
  it('gửi số tiền âm kèm lý do rồi đóng hộp thoại', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ id: 'adj-1', amountVnd: -1350000, adjustment: true }), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    const onClose = renderDialog()

    await userEvent.type(screen.getByLabelText('Số tiền giảm (VND)'), '1350000')
    await userEvent.type(screen.getByLabelText('Lý do điều chỉnh'), 'Nhập nhầm số tiền')
    await userEvent.click(screen.getByRole('button', { name: 'Ghi điều chỉnh' }))

    await vi.waitFor(() => expect(onClose).toHaveBeenCalled())
    const [url, init] = fetchSpy.mock.calls[0]
    expect(String(url)).toContain('/enrollments/enr-1/payments/adjustments')
    expect(JSON.parse(String(init?.body))).toEqual({
      amountVnd: -1350000,
      note: 'Nhập nhầm số tiền',
    })
  })

  it('không cho giảm quá tổng đã thu', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    renderDialog()

    await userEvent.type(screen.getByLabelText('Số tiền giảm (VND)'), '2000000')
    await userEvent.type(screen.getByLabelText('Lý do điều chỉnh'), 'Nhập nhầm')

    expect(screen.getByText(/Không được giảm quá tổng đã thu/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ghi điều chỉnh' })).toBeDisabled()
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('bắt buộc lý do và nhắc không ghi thông tin cá nhân', async () => {
    renderDialog()

    await userEvent.type(screen.getByLabelText('Số tiền giảm (VND)'), '100000')

    expect(screen.getByRole('button', { name: 'Ghi điều chỉnh' })).toBeDisabled()
    expect(screen.getByLabelText('Lý do điều chỉnh')).toHaveAccessibleDescription(
      /Không ghi tên, số điện thoại/,
    )
  })
})
