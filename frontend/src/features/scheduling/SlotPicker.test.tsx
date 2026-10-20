import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import SlotPicker from './SlotPicker'
import * as schedulingApi from '@/lib/api/scheduling'

const mockSlots: schedulingApi.Slot[] = [
  {
    startsAt: '2026-11-01T09:00:00Z',
    endsAt: '2026-11-01T10:00:00Z',
    durationMinutes: 60,
  },
  {
    startsAt: '2026-11-01T14:00:00Z',
    endsAt: '2026-11-01T15:00:00Z',
    durationMinutes: 60,
  },
]

describe('SlotPicker component', () => {
  it('renders date strip and fetches slots', async () => {
    vi.spyOn(schedulingApi, 'fetchSlots').mockResolvedValue(mockSlots)
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    const onSelectSlot = vi.fn()

    render(
      <QueryClientProvider client={queryClient}>
        <SlotPicker enrollmentId="enr-1" selectedSlot={null} onSelectSlot={onSelectSlot} />
      </QueryClientProvider>,
    )

    // Verify header exists
    expect(screen.getByText(/Bước 1: Chọn ngày học/i)).toBeInTheDocument()

    // Wait for slots to finish loading
    await waitFor(() => {
      expect(screen.queryByText(/Đang tìm các khung giờ trống/i)).not.toBeInTheDocument()
    })

    // Click on a slot (09:00 UTC formatted in local time)
    const slotBtn = await screen.findByRole('button', { name: /16:00/i })
    await userEvent.click(slotBtn)
    expect(onSelectSlot).toHaveBeenCalledWith(mockSlots[0])
  })
})
