import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import ProgressPage from './ProgressPage'
import { shortDate } from './skills'

vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'stu-1', role: 'STUDENT', timezone: 'Asia/Ho_Chi_Minh' } }),
}))

// Recharts cần kích thước thật; biểu đồ được kiểm trên trình duyệt
vi.mock('./SkillChart', () => ({
  default: ({ points }: { points: unknown[] }) => <div>Biểu đồ {points.length} điểm</div>,
}))

const progress = {
  skillSeries: [
    {
      sessionId: 'ses-1',
      date: '2026-11-02',
      listening: 3,
      speaking: null,
      reading: 4,
      writing: null,
      grammarVocab: null,
    },
  ],
  pathCompletion: 25,
  recentReports: [
    {
      sessionId: 'ses-1',
      sessionEndsAt: '2026-11-02T12:00:00Z',
      submittedAt: '2026-11-02T13:00:00Z',
      scores: { listening: 3, reading: 4 },
      comment: 'Nghe tốt hơn',
    },
  ],
}
const proficiency = {
  current: {
    id: 'a-1',
    studentId: 'stu-1',
    assessedBy: 't',
    assessedAt: '2026-10-26T00:00:00Z',
    type: 'PLACEMENT',
    listening: 'B1',
    speaking: 'A2',
    reading: 'B1',
    writing: 'A2',
    grammarVocab: 'B2',
  },
  history: [],
  needsConsulting: false,
}

function mockApi(progressBody: unknown, proficiencyBody: unknown = proficiency) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const body = String(input).endsWith('/progress') ? progressBody : proficiencyBody
    return new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  })
}

function renderPage(studentId?: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ProgressPage studentId={studentId} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ProgressPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('học viên xem của chính mình: đủ CEFR, %, biểu đồ, bảng, nhận xét', async () => {
    const fetchMock = mockApi(progress)
    renderPage()

    expect(await screen.findByText('Biểu đồ 1 điểm')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students/stu-1/progress'),
      expect.any(Object),
    )
    expect(screen.getByRole('heading', { name: 'Tiến bộ' })).toBeInTheDocument()
    expect(screen.getByText('25%')).toBeInTheDocument()
    expect(await screen.findByText('B2')).toBeInTheDocument()
    expect(screen.getByText('Nghe tốt hơn')).toBeInTheDocument()
    const row = screen.getByRole('cell', { name: '02/11' }).closest('tr')!
    expect(
      within(row)
        .getAllByRole('cell')
        .map((c) => c.textContent),
    ).toEqual(['02/11', '3', '—', '4', '—', '—'])
  })

  it('pathCompletion null và chưa có phiếu không vỡ giao diện', async () => {
    mockApi(
      { skillSeries: [], pathCompletion: null, recentReports: [] },
      { current: null, history: [], needsConsulting: true },
    )
    renderPage()

    expect(await screen.findByText('Chưa có lộ trình hoặc chủ đề.')).toBeInTheDocument()
    expect(screen.getByText('Chưa có phiếu đánh giá nào.')).toBeInTheDocument()
    expect(screen.getByText('Chưa có nhận xét nào.')).toBeInTheDocument()
    expect(await screen.findByText('Chưa có đánh giá năng lực.')).toBeInTheDocument()
    expect(screen.queryByText(/Biểu đồ/)).not.toBeInTheDocument()
  })

  it('lộ trình 0% vẫn hiện số, không coi là chưa có', async () => {
    mockApi({ ...progress, pathCompletion: 0 })
    renderPage()

    expect(await screen.findByText('0%')).toBeInTheDocument()
  })

  it('tab ở hồ sơ: gọi theo học viên đang xem, không có tiêu đề trang', async () => {
    const fetchMock = mockApi(progress)
    renderPage('stu-9')

    await screen.findByText('Biểu đồ 1 điểm')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/students/stu-9/progress'),
      expect.any(Object),
    )
    expect(screen.queryByRole('heading', { name: 'Tiến bộ' })).not.toBeInTheDocument()
  })

  it('shortDate không đổi múi giờ', () => {
    expect(shortDate('2026-11-02')).toBe('02/11')
  })
})
