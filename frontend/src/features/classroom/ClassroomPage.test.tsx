import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router'
import { DisconnectReason } from 'livekit-client'
import ClassroomPage from './ClassroomPage'

const auth = vi.hoisted(() => ({ role: 'TEACHER' }))

vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'me-1', role: auth.role, fullName: 'Người dùng' } }),
}))

// Phòng thật cần LiveKit; kết nối thật được kiểm bằng hai trình duyệt
vi.mock('@livekit/components-react', () => ({
  LiveKitRoom: ({ onDisconnected }: { onDisconnected: (reason?: number) => void }) => (
    <div>
      <button onClick={() => onDisconnected(DisconnectReason.CLIENT_INITIATED)}>Tự rời</button>
      <button onClick={() => onDisconnected(DisconnectReason.SIGNAL_CLOSE)}>Mất mạng</button>
    </div>
  ),
}))
vi.mock('./ClassroomRoom', () => ({ default: () => null }))
vi.mock('@livekit/components-styles', () => ({}))

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function renderPage(role: string, join: () => Response) {
  auth.role = role
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) =>
    String(input).endsWith('/join')
      ? join()
      : json(200, { id: 'ses-1', startsAt: '2099-01-01T00:00:00Z', status: 'BOOKED' }),
  )
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/sessions/ses-1/classroom']}>
        <Routes>
          <Route path="/sessions/:id/classroom" element={<ClassroomPage />} />
          <Route path="/student/schedule" element={<p>Lịch học viên</p>} />
          <Route path="/teacher/schedule" element={<p>Lịch giáo viên</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const joined = () => json(200, { token: 't', serverUrl: 'ws://x', roomName: 'r' })

describe('ClassroomPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('giáo viên tự rời phòng thì tới bước chốt kết quả', async () => {
    renderPage('TEACHER', joined)

    await userEvent.click(await screen.findByRole('button', { name: 'Tự rời' }))

    expect(await screen.findByText('Chốt kết quả buổi học')).toBeInTheDocument()
  })

  it('học viên tự rời phòng thì về lịch học', async () => {
    renderPage('STUDENT', joined)

    await userEvent.click(await screen.findByRole('button', { name: 'Tự rời' }))

    expect(await screen.findByText('Lịch học viên')).toBeInTheDocument()
  })

  it('mất mạng không phải rời phòng: không hỏi chốt kết quả, cho vào lại', async () => {
    renderPage('TEACHER', joined)

    await userEvent.click(await screen.findByRole('button', { name: 'Mất mạng' }))

    expect(await screen.findByText('Mất kết nối phòng học')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Vào lại phòng' })).toBeInTheDocument()
    expect(screen.queryByText('Chốt kết quả buổi học')).not.toBeInTheDocument()
  })

  it('phòng chưa mở: báo đúng lý do từ backend', async () => {
    renderPage('STUDENT', () => json(409, { code: 'ROOM_NOT_OPEN', message: 'x' }))

    expect(await screen.findByText('Không vào được phòng học')).toBeInTheDocument()
    expect(screen.getByText(/Phòng học chỉ mở từ 10 phút trước giờ học/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Quay lại lịch học' }))
    expect(await screen.findByText('Lịch học viên')).toBeInTheDocument()
  })
})
