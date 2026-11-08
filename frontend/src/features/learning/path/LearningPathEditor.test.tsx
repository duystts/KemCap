import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import LearningPathEditor from './LearningPathEditor'
import type { LearningPathDto } from '@/lib/api/learning'

const path: LearningPathDto = {
  id: 'path-1',
  studentId: 'stu-1',
  title: 'Lộ trình giao tiếp',
  status: 'ACTIVE',
  completionPercent: 0,
  version: 3,
  milestones: [
    {
      id: 'm-1',
      position: 1,
      title: 'Nền tảng',
      targetDate: '2026-12-01',
      topics: [
        { id: 't-1', position: 1, title: 'Chào hỏi', objective: null, status: 'DONE' },
        { id: 't-2', position: 2, title: 'Giới thiệu', objective: null, status: 'PLANNED' },
      ],
    },
    { id: 'm-2', position: 2, title: 'Nâng cao', targetDate: null, topics: [] },
  ],
}

function renderEditor(existingPath: LearningPathDto | null = path, onClose = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <LearningPathEditor studentId="stu-1" existingPath={existingPath} open onClose={onClose} />
    </QueryClientProvider>,
  )
  return onClose
}

function mockSave() {
  return vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    new Response(JSON.stringify(path), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

afterEach(() => vi.restoreAllMocks())

describe('LearningPathEditor', () => {
  it('sắp xếp, xóa rồi gửi cả cây theo thứ tự mới kèm lý do và version', async () => {
    const fetchSpy = mockSave()
    const onClose = renderEditor()

    await userEvent.click(screen.getByRole('button', { name: 'Đưa chặng 2 lên' }))
    // chặng "Nền tảng" giờ là chặng 2
    await userEvent.click(screen.getByRole('button', { name: 'Đưa chủ đề 2.2 lên' }))
    await userEvent.click(screen.getByRole('button', { name: 'Xóa chủ đề 2.2' }))
    await userEvent.click(screen.getByRole('button', { name: 'Lưu lộ trình' }))
    await userEvent.type(screen.getByLabelText(/Lý do điều chỉnh/), 'Đổi thứ tự theo yêu cầu')
    await userEvent.click(screen.getByRole('button', { name: 'Xác nhận lưu' }))

    await vi.waitFor(() => expect(onClose).toHaveBeenCalled())
    const [url, init] = fetchSpy.mock.calls[0]
    expect(String(url)).toContain('/learning-paths/path-1')
    expect(init?.method).toBe('PUT')
    const body = JSON.parse(String(init?.body))
    expect(body.reason).toBe('Đổi thứ tự theo yêu cầu')
    expect(body.version).toBe(3)
    expect(body.milestones.map((m: { id: string }) => m.id)).toEqual(['m-2', 'm-1'])
    expect(body.milestones[1].topics).toEqual([
      { id: 't-2', title: 'Giới thiệu', objective: undefined },
    ])
  })

  it('chặng hoặc chủ đề trống tên thì không gửi', async () => {
    const fetchSpy = mockSave()
    renderEditor()

    await userEvent.clear(screen.getByLabelText('Tên chủ đề 1.1'))
    await userEvent.click(screen.getByRole('button', { name: 'Lưu lộ trình' }))

    expect(screen.queryByLabelText(/Lý do điều chỉnh/)).not.toBeInTheDocument()
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('tạo mới không hỏi lý do, chủ đề mặc định không có mục tiêu giả', async () => {
    const fetchSpy = mockSave()
    renderEditor(null)

    expect(screen.getByLabelText('Mục tiêu chủ đề 1.1')).toHaveValue('')
    await userEvent.click(screen.getByRole('button', { name: 'Lưu lộ trình' }))

    await vi.waitFor(() => expect(fetchSpy).toHaveBeenCalled())
    const [url, init] = fetchSpy.mock.calls[0]
    expect(String(url)).toContain('/students/stu-1/learning-path')
    expect(init?.method).toBe('POST')
  })
})
