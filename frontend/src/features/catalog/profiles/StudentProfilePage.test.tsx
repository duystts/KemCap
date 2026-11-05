import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router'
import StudentProfilePage from './StudentProfilePage'

const auth = vi.hoisted(() => ({ role: 'TEACHER' }))

vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'me-1', role: auth.role } }),
}))

const enrollment = {
  id: 'enr-1',
  sessionsTotal: 12,
  sessionMinutes: 45,
  sessionsConsumed: 0,
  sessionsReserved: 0,
  sessionsRemaining: 12,
  status: 'ACTIVE',
}

async function openEnrollmentsTab() {
  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
    const body = String(input).includes('/enrollments') ? [enrollment] : []
    return new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  })
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/students/stu-1']}>
        <Routes>
          <Route path="/students/:id" element={<StudentProfilePage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  await userEvent.click(await screen.findByRole('button', { name: /Gói học & Ghi danh \(1\)/ }))
}

describe('StudentProfilePage: link trang ghi danh', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('giáo vụ thấy link', async () => {
    auth.role = 'ADMIN'
    await openEnrollmentsTab()
    expect(screen.getByRole('link', { name: /Chi tiết & Học phí/ })).toHaveAttribute(
      'href',
      '/admin/enrollments/enr-1',
    )
  })

  it('giáo viên không thấy link', async () => {
    auth.role = 'TEACHER'
    await openEnrollmentsTab()
    expect(screen.getByText('Gói 12 buổi (45p)')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Chi tiết & Học phí/ })).not.toBeInTheDocument()
  })
})
