import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router'
import DataSubjectRequestsPage from './DataSubjectRequestsPage'
import PersonalDataSummaryPage from './PersonalDataSummaryPage'

vi.mock('@/app/AuthProvider', () => ({
  useAuth: () => ({ me: { id: 'adm', role: 'ADMIN', timezone: 'Asia/Ho_Chi_Minh' } }),
}))

const student = {
  id: 'stu-1',
  email: 'an@x.vn',
  fullName: 'Lê Hoàng An',
  role: 'STUDENT',
  status: 'ACTIVE',
  anonymized: false,
}
const request = {
  id: 'r-1',
  studentId: 'stu-1',
  requesterName: 'Phụ huynh An',
  type: 'ERASURE',
  receivedOn: '2026-11-05',
  status: 'RECEIVED',
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function renderRoute(node: React.ReactNode, path = '/') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/students/:id" element={node} />
          <Route path="/" element={node} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('DataSubjectRequestsPage', () => {
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

  function mockApi(requests: unknown[], users: unknown[] = [student]) {
    return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (init?.method === 'POST') return json(200, request)
      if (url.includes('/users')) return json(200, users)
      return json(200, requests)
    })
  }
  const body = (fetchMock: ReturnType<typeof mockApi>, path: string) => {
    const call = fetchMock.mock.calls.find(
      ([url, init]) => init?.method === 'POST' && String(url).endsWith(path),
    )
    return call ? JSON.parse(String(call[1]!.body ?? 'null')) : undefined
  }

  it('ẩn danh hóa phải qua xác nhận "không khôi phục được" và gửi confirm true', async () => {
    const fetchMock = mockApi([request])
    renderRoute(<DataSubjectRequestsPage />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Ẩn danh hóa' }))
    expect(body(fetchMock, '/r-1/anonymize')).toBeUndefined()
    const dialog = screen.getByRole('dialog', { hidden: true })
    expect(dialog).toHaveTextContent('không khôi phục được')
    expect(dialog).toHaveTextContent('Lê Hoàng An')
    await user.click(
      within(dialog).getByRole('button', { name: 'Ẩn danh hóa vĩnh viễn', hidden: true }),
    )

    await waitFor(() => expect(body(fetchMock, '/r-1/anonymize')).toEqual({ confirm: true }))
  })

  it('hủy xác nhận thì không gọi API', async () => {
    const fetchMock = mockApi([request])
    renderRoute(<DataSubjectRequestsPage />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Ẩn danh hóa' }))
    const dialog = screen.getByRole('dialog', { hidden: true })
    await user.click(within(dialog).getByRole('button', { name: 'Hủy bỏ', hidden: true }))

    expect(body(fetchMock, '/r-1/anonymize')).toBeUndefined()
  })

  it('yêu cầu xem: có bản tóm tắt và hoàn tất, không có ẩn danh hóa', async () => {
    const fetchMock = mockApi([{ ...request, type: 'ACCESS' }])
    renderRoute(<DataSubjectRequestsPage />)
    const user = userEvent.setup()

    const link = await screen.findByRole('link', { name: 'Mở bản tóm tắt' })
    expect(link).toHaveAttribute('href', '/admin/privacy/students/stu-1')
    expect(screen.queryByRole('button', { name: 'Ẩn danh hóa' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Hoàn tất' }))
    await waitFor(() => expect(body(fetchMock, '/r-1/complete-access')).toBeNull())
  })

  it('từ chối bắt buộc có lý do', async () => {
    const fetchMock = mockApi([request])
    renderRoute(<DataSubjectRequestsPage />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Từ chối' }))
    const confirm = screen.getByRole('button', { name: 'Xác nhận từ chối' })
    expect(confirm).toBeDisabled()
    await user.type(screen.getByLabelText('Lý do từ chối'), ' Không xác minh được ')
    await user.click(confirm)

    await waitFor(() =>
      expect(body(fetchMock, '/r-1/reject')).toEqual({ resolutionNote: 'Không xác minh được' }),
    )
  })

  it('yêu cầu đã xử lý không còn thao tác; học viên đã ẩn danh không chọn được và hiện nhãn', async () => {
    mockApi(
      [{ ...request, status: 'COMPLETED', resolvedAt: '2026-11-05T10:00:00Z' }],
      [{ ...student, anonymized: true, fullName: 'x' }],
    )
    renderRoute(<DataSubjectRequestsPage />)

    expect(await screen.findByRole('cell', { name: 'Học viên đã ẩn danh' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ẩn danh hóa' })).not.toBeInTheDocument()
    expect(within(screen.getByLabelText('Học viên')).queryAllByRole('option')).toHaveLength(1)
  })

  it('ghi nhận: đủ thông tin mới gửi được', async () => {
    const fetchMock = mockApi([])
    renderRoute(<DataSubjectRequestsPage />)
    const user = userEvent.setup()

    const submit = await screen.findByRole('button', { name: 'Ghi nhận' })
    expect(submit).toBeDisabled()
    await screen.findByRole('option', { name: 'Lê Hoàng An' })
    await user.selectOptions(screen.getByLabelText('Học viên'), 'stu-1')
    await user.type(screen.getByLabelText('Người yêu cầu'), ' Phụ huynh ')
    await user.selectOptions(screen.getByLabelText('Loại yêu cầu'), 'ERASURE')
    await user.click(submit)

    await waitFor(() =>
      expect(body(fetchMock, '/data-subject-requests')).toMatchObject({
        studentId: 'stu-1',
        requesterName: 'Phụ huynh',
        type: 'ERASURE',
      }),
    )
  })
})

describe('PersonalDataSummaryPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mockApi(override: (url: string) => Response | undefined = () => undefined) {
    return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      const custom = override(url)
      if (custom) return custom
      if (/policy-consents|\/notifications|teacher-history|outcome-history/.test(url))
        return json(200, [])
      if (url.includes('/guardian-consent')) return json(404, { code: 'NOT_FOUND', message: 'x' })
      if (url.includes('/learning-path')) return json(404, { code: 'NOT_FOUND', message: 'x' })
      if (url.includes('/profile')) return json(200, { userId: 'stu-1', occupation: 'Kỹ sư' })
      if (url.includes('/proficiency')) return json(200, { history: [], needsConsulting: false })
      if (url.includes('/progress'))
        return json(200, { skillSeries: [], pathCompletion: null, recentReports: [] })
      if (url.includes('/users/stu-1')) return json(200, student)
      if (url.includes('/payments'))
        return json(200, {
          enrollmentId: 'e-1',
          amountDueVnd: 3000000,
          amountPaidVnd: 1000000,
          debtVnd: 2000000,
          payments: [],
        })
      if (url.includes('/enrollments'))
        return json(200, [
          {
            id: 'e-1',
            teacherId: 't-1',
            sessionsTotal: 12,
            sessionMinutes: 45,
            status: 'ACTIVE',
          },
        ])
      if (url.includes('/users')) return json(200, [{ id: 't-1', fullName: 'Nguyễn Thị Lan' }])
      return json(200, [])
    })
  }

  it('ghép đủ dữ liệu; 404 của lộ trình và giám hộ không phải lỗi', async () => {
    mockApi()
    renderRoute(<PersonalDataSummaryPage />, '/students/stu-1')

    expect(await screen.findByText('Lê Hoàng An')).toBeInTheDocument()
    expect(screen.getByText('an@x.vn')).toBeInTheDocument()
    expect(screen.getByText('Kỹ sư')).toBeInTheDocument()
    expect(screen.getByText('Nguyễn Thị Lan')).toBeInTheDocument()
    expect(await screen.findByText((2000000).toLocaleString('vi-VN') + ' đ')).toBeInTheDocument()
    expect(screen.getByText('Chưa có lộ trình.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'In / lưu PDF' })).toBeEnabled()
  })

  it('đủ mọi nhận xét, không chỉ 5 phiếu gần nhất; có đánh giá sao', async () => {
    mockApi((url) => {
      if (url.includes('/progress')) {
        return json(200, {
          skillSeries: [{ sessionId: 'old-1', date: '2026-01-02', listening: 2 }],
          pathCompletion: null,
          recentReports: [],
        })
      }
      if (url.endsWith('/sessions/old-1/report')) {
        return json(200, { sessionId: 'old-1', comment: 'Nhận xét rất cũ' })
      }
      if (url.endsWith('/sessions/ses-9/feedback')) {
        return json(200, { sessionId: 'ses-9', stars: 4, comment: 'Cô dạy hay' })
      }
      if (url.endsWith('/feedback')) return json(404, { code: 'NOT_FOUND', message: 'x' })
      if (url.includes('/sessions?')) {
        return json(200, [
          { id: 'ses-9', teacherId: 't-1', startsAt: '2026-01-02T10:00:00Z', status: 'COMPLETED' },
          { id: 'ses-8', teacherId: 't-1', startsAt: '2026-01-03T10:00:00Z', status: 'COMPLETED' },
        ])
      }
      return undefined
    })
    renderRoute(<PersonalDataSummaryPage />, '/students/stu-1')

    expect(await screen.findByText('Nhận xét rất cũ')).toBeInTheDocument()
    expect(await screen.findByText('Cô dạy hay')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'In / lưu PDF' })).toBeEnabled()
  })

  it('có các mục FR-031: đồng ý chính sách, thông báo, đổi giáo viên, lý do hủy, ghi chú chốt', async () => {
    mockApi((url) => {
      if (url.includes('/policy-consents')) {
        return json(200, [{ policyVersion: 'v2026', acceptedAt: '2026-01-02T10:00:00Z' }])
      }
      if (url.includes('/users/stu-1/notifications')) {
        return json(200, [{ id: 'n-1', title: 'Nhắc lịch học', createdAt: '2026-01-02T10:00:00Z' }])
      }
      if (url.includes('/teacher-history')) {
        return json(200, [
          { toTeacherId: 't-1', reason: 'Đổi theo nguyện vọng', changedAt: '2026-01-02T10:00:00Z' },
        ])
      }
      if (url.endsWith('/outcome-history')) {
        return json(200, [{ toStatus: 'COMPLETED', note: 'Vào muộn 10 phút', createdAt: 'x' }])
      }
      if (url.endsWith('/feedback')) return json(404, { code: 'NOT_FOUND', message: 'x' })
      if (url.includes('/sessions?')) {
        return json(200, [
          {
            id: 'ses-7',
            teacherId: 't-1',
            startsAt: '2026-01-02T10:00:00Z',
            status: 'CANCELLED_LATE',
            cancelReason: 'Em bị ốm',
          },
        ])
      }
      return undefined
    })
    renderRoute(<PersonalDataSummaryPage />, '/students/stu-1')

    for (const text of [
      'v2026',
      'Nhắc lịch học',
      'Đổi theo nguyện vọng',
      'Em bị ốm',
      'Vào muộn 10 phút',
    ]) {
      expect(await screen.findByText(text)).toBeInTheDocument()
    }
  })

  it('in gọi window.print', async () => {
    mockApi()
    const print = vi.spyOn(window, 'print').mockImplementation(() => {})
    renderRoute(<PersonalDataSummaryPage />, '/students/stu-1')
    const user = userEvent.setup()

    await screen.findByText('Lê Hoàng An')
    await user.click(screen.getByRole('button', { name: 'In / lưu PDF' }))

    expect(print).toHaveBeenCalled()
  })

  it('thiếu một nguồn dữ liệu thì không hiện bản dở dang, không cho in', async () => {
    mockApi((url) =>
      url.includes('/assignments')
        ? json(500, { code: 'INTERNAL_ERROR', message: 'x' })
        : undefined,
    )
    renderRoute(<PersonalDataSummaryPage />, '/students/stu-1')

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải đủ dữ liệu')
    expect(screen.queryByText('Lê Hoàng An')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'In / lưu PDF' })).toBeDisabled()
  })
})
