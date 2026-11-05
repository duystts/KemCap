import { expect, test, type Page } from '@playwright/test'
import {
  acceptInvite,
  apiCall,
  apiToken,
  birthDateYearsAgo,
  createUser,
  dialog,
  login,
  mailsTo,
  moveSession,
  openFirstDayWithSlots,
  RUN,
} from './helpers'

/**
 * Smoke test kịch bản demo (T142): quickstart bước 1–5, 8, 11, 15–17.
 * Bước 16 không tự động phần đổi `app.privacy.policy-version` vì cần khởi động lại backend.
 */
test.describe.configure({ mode: 'serial' })

const TEACHER_EMAIL = `gv.moi.${RUN}@demo.local`
const TEACHER_NAME = `Giáo Viên Mới ${RUN}`
const STUDENT_EMAIL = `hv.moi.${RUN}@demo.local`
const STUDENT_NAME = `Học Viên Mới ${RUN}`
const COURSE_NAME = `Giao tiếp ${RUN}`
const PACKAGE_NAME = `Giao tiếp 12 buổi × 45 phút ${RUN}`
const PRICE = 3_000_000
const vnd = (amount: number) => `${amount.toLocaleString('vi-VN')} đ`

let admin: Page
let adminToken: string

test.beforeAll(async ({ browser }) => {
  admin = await login(browser, 'admin@demo.local')
  adminToken = await apiToken('admin@demo.local')
})

test('bước 1: tạo giáo viên, học viên; người nhận đặt mật khẩu và đăng nhập được', async ({
  browser,
}) => {
  await createUser(admin, { role: 'TEACHER', name: TEACHER_NAME, email: TEACHER_EMAIL })
  await createUser(admin, {
    role: 'STUDENT',
    name: STUDENT_NAME,
    email: STUDENT_EMAIL,
    birthDate: birthDateYearsAgo(25),
  })

  for (const [email, home] of [
    [TEACHER_EMAIL, '/teacher'],
    [STUDENT_EMAIL, '/student'],
  ]) {
    const page = await acceptInvite(browser, email)
    await page.getByRole('checkbox').check()
    await page.getByRole('button', { name: 'Kích hoạt tài khoản' }).click()
    await expect(page.getByRole('heading', { name: 'Kích hoạt thành công' })).toBeVisible()
    await page.context().close()
    const loggedIn = await login(browser, email)
    await expect(loggedIn).toHaveURL(new RegExp(`${home}$`))
    await loggedIn.context().close()
  }
})

test('bước 2: tạo gói, ghi danh; chờ thanh toán; giáo viên chỉ thấy học viên của mình', async ({
  browser,
}) => {
  await admin.goto('/admin/courses')
  await admin.getByRole('button', { name: 'Tạo khóa học mới' }).click()
  await dialog(admin).getByPlaceholder('Ví dụ: IELTS Intensive 1 kèm 1').fill(COURSE_NAME)
  await dialog(admin).getByRole('button', { name: 'Tạo khóa học', exact: true }).click()
  await expect(admin.getByText(COURSE_NAME, { exact: true })).toBeVisible()

  await admin.goto('/admin/packages')
  await admin.getByRole('button', { name: 'Tạo gói học mới' }).click()
  const pkg = dialog(admin)
  await pkg.locator('select').first().selectOption({ label: COURSE_NAME })
  await pkg.getByPlaceholder('Ví dụ: Gói 10 buổi (60 phút)').fill(PACKAGE_NAME)
  await pkg.locator('select').nth(1).selectOption('45')
  const numbers = pkg.locator('input[type=number]')
  await numbers.nth(0).fill('12')
  await numbers.nth(1).fill(String(PRICE))
  await numbers.nth(2).fill('90')
  await pkg.getByRole('button', { name: 'Tạo gói học', exact: true }).click()
  await expect(admin.getByText(PACKAGE_NAME)).toBeVisible()

  const students = (await apiCall(adminToken, '/students')).body
  const student = students.find((s: { email: string }) => s.email === STUDENT_EMAIL)
  await admin.goto(`/admin/students/${student.id}`)
  await admin.getByRole('button', { name: 'Tạo ghi danh', exact: true }).click()
  const enroll = dialog(admin)
  // Mở từ hồ sơ học viên nên không có ô chọn học viên: chỉ còn gói và giáo viên
  const [packageSelect, teacherSelect] = [
    enroll.locator('select').nth(0),
    enroll.locator('select').nth(1),
  ]
  const packageOption = packageSelect.locator('option', { hasText: PACKAGE_NAME })
  await packageSelect.selectOption((await packageOption.getAttribute('value'))!)
  const teacherOption = teacherSelect.locator('option', { hasText: TEACHER_NAME })
  await teacherSelect.selectOption((await teacherOption.getAttribute('value'))!)
  await enroll.getByRole('button', { name: 'Xác nhận ghi danh' }).click()
  await expect(enroll.getByRole('button', { name: 'Xác nhận ghi danh' })).toBeHidden()

  await admin.getByRole('button', { name: /Gói học & Ghi danh/ }).click()
  await expect(admin.getByText('Chờ thanh toán').first()).toBeVisible()

  const teacher = await login(browser, TEACHER_EMAIL)
  await teacher.goto('/teacher/students')
  await expect(teacher.getByText(STUDENT_NAME)).toBeVisible()
  await expect(teacher.locator('tbody tr')).toHaveCount(1)
  await teacher.context().close()
})

test('bước 3: thu 50% học phí thì gói kích hoạt, còn 12 buổi, công nợ bằng 50%', async () => {
  await admin
    .getByRole('link', { name: /Chi tiết & Học phí/ })
    .first()
    .click()
  await admin.waitForURL(/\/admin\/enrollments\//)
  await admin.getByRole('button', { name: 'Thu học phí' }).first().click()
  const payment = dialog(admin)
  await payment.locator('input[type=number]').fill(String(PRICE / 2))
  await payment.getByRole('button', { name: 'Xác nhận thu học phí' }).click()
  await expect(payment.getByRole('button', { name: 'Xác nhận thu học phí' })).toBeHidden()

  await expect(admin.getByText('Đang dùng').first()).toBeVisible()
  await expect(admin.getByText(vnd(PRICE / 2)).first()).toBeVisible()
  const enrollmentId = admin.url().split('/').pop()!
  const statement = (await apiCall(adminToken, `/enrollments/${enrollmentId}/payments`)).body
  expect(statement.debtVnd).toBe(PRICE / 2)
  const enrollment = (await apiCall(adminToken, `/enrollments/${enrollmentId}`)).body
  expect(enrollment.sessionsRemaining).toBe(12)
})

test('bước 4: giáo viên khai lịch rảnh T2–T6 18:00–21:00; học viên thấy khung giờ bước 30 phút', async ({
  browser,
}) => {
  const teacher = await login(browser, TEACHER_EMAIL)
  await teacher.goto('/teacher/availability')
  for (const day of ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu']) {
    await teacher.getByRole('button', { name: new RegExp(`^${day}`) }).click()
    const times = teacher.locator('input[type=time]')
    await times.nth(0).fill('18:00')
    await times.nth(1).fill('21:00')
    await teacher.getByRole('button', { name: 'Thêm khung giờ' }).click()
  }
  await teacher.getByRole('button', { name: 'Lưu thay đổi' }).click()
  await expect(teacher.getByText('Đã lưu lịch rảnh định kỳ thành công!')).toBeVisible()
  await teacher.context().close()

  const student = await login(browser, STUDENT_EMAIL)
  await student.goto('/student/book')
  const starts = await openFirstDayWithSlots(student)
  expect(starts.length).toBeGreaterThan(1)
  const minutes = starts.map((t) => Number(t.slice(0, 2)) * 60 + Number(t.slice(3)))
  for (let i = 1; i < minutes.length; i++) expect(minutes[i] - minutes[i - 1]).toBe(30)
  await student.context().close()
})

test('bước 5: hai tab đặt cùng khung giờ: tab đầu thành công, tab sau SLOT_TAKEN; có email xác nhận', async ({
  browser,
}) => {
  const first = await login(browser, STUDENT_EMAIL)
  const second = await first.context().newPage()
  const mailsBefore = (await mailsTo(STUDENT_EMAIL)).length

  for (const page of [first, second]) {
    await page.goto('/student/book')
    await openFirstDayWithSlots(page)
    await page.getByTestId('slot-item').first().click()
  }
  await first.getByRole('button', { name: 'Xác nhận Đặt buổi này' }).click()
  await first.waitForURL('**/student/schedule')

  await second.getByRole('button', { name: 'Xác nhận Đặt buổi này' }).click()
  await expect(second.getByText(/Khung giờ này vừa có người đặt/)).toBeVisible()

  await first.goto('/student/packages')
  await expect(first.getByText('Còn lại').locator('..')).toContainText('11')

  await expect(async () => {
    expect((await mailsTo(STUDENT_EMAIL)).length).toBeGreaterThan(mailsBefore)
    expect((await mailsTo(TEACHER_EMAIL)).length).toBeGreaterThan(1)
  }).toPass({ timeout: 20_000 })
  await first.context().close()
})

test('bước 8: giáo viên chốt buổi, điền phiếu đúng hạn; đã trừ buổi; học viên nhận thông báo', async ({
  browser,
}) => {
  const teacherToken = await apiToken('teacher1@demo.local')
  const studentToken = await apiToken('student1@demo.local')
  const me = (await apiCall(studentToken, '/me')).body
  const pending = (
    await apiCall(teacherToken, '/sessions?status=BOOKED&outcomePending=true')
  ).body.find((s: { studentId: string }) => s.studentId === me.id)
  expect(pending, 'seed có buổi chưa chốt của student1: cần chạy trên database mới').toBeTruthy()
  moveSession(pending.id, '-2 hours', '-1 hours')
  const enrollmentOf = async () =>
    (await apiCall(adminToken, `/enrollments?studentId=${me.id}`)).body.find(
      (e: { id: string }) => e.id === pending.enrollmentId,
    )
  const before = await enrollmentOf()

  const teacher = await login(browser, 'teacher1@demo.local')
  await teacher.goto(`/sessions/${pending.id}`)
  await teacher.getByRole('button', { name: 'Chốt kết quả' }).click()
  await dialog(teacher).getByText('Hoàn thành tốt').click()
  await dialog(teacher)
    .getByRole('button', { name: /Xác nhận chốt/ })
    .click()

  await teacher
    .getByRole('checkbox', { name: /^Đã dạy:/ })
    .nth(0)
    .check()
  await teacher
    .getByRole('checkbox', { name: /^Đã dạy:/ })
    .nth(1)
    .check()
  for (const [skill, score] of [
    ['Nghe', '4'],
    ['Nói', '3'],
    ['Đọc', '5'],
    ['Viết', '4'],
  ]) {
    await teacher
      .getByRole('group', { name: skill, exact: true })
      .getByRole('button', { name: score, exact: true })
      .click()
  }
  const comment = `Nhận xét demo ${RUN}`
  await teacher.getByLabel('Nhận xét').fill(comment)
  await teacher.getByRole('button', { name: 'Thêm bài tập' }).click()
  await teacher.getByLabel('Tiêu đề bài tập 1').fill(`Bài tập demo ${RUN}`)
  await teacher.getByRole('button', { name: 'Nộp phiếu' }).click()
  await teacher.locator('dialog[open]').getByRole('button', { name: 'Nộp phiếu' }).click()

  await expect(teacher.getByText(/Đã nộp lúc/)).toBeVisible()
  await expect(teacher.getByText(/trễ hạn/)).toBeHidden()
  await teacher.context().close()

  const after = await enrollmentOf()
  expect(after.sessionsConsumed).toBe(before.sessionsConsumed + 1)
  await expect(async () => {
    const notifications = (await apiCall(studentToken, '/notifications')).body.items
    expect(notifications.map((n: { type: string }) => n.type)).toContain('REPORT_SUBMITTED')
  }).toPass({ timeout: 20_000 })
})

test('bước 11: học viên mở Tiến bộ thấy CEFR, % lộ trình, biểu đồ, nhận xét', async ({
  browser,
}) => {
  const student = await login(browser, 'student1@demo.local')
  await student.goto('/student/progress')
  await expect(student.getByRole('heading', { name: 'Tiến bộ', exact: true })).toBeVisible()
  await expect(student.getByRole('heading', { name: 'Trình độ CEFR hiện tại' })).toBeVisible()
  await expect(
    student
      .locator('dd')
      .filter({ hasText: /^(A1|A2|B1|B2|C1|C2)$/ })
      .first(),
  ).toBeVisible()
  await expect(student.getByText(/^\d+%$/)).toBeVisible()
  await expect(student.locator('.recharts-line-dot').first()).toBeVisible()
  await expect(student.getByText(`Nhận xét demo ${RUN}`)).toBeVisible()
  await student.context().close()
})

const CHILD_EMAIL = `tre.${RUN}@demo.local`

test('bước 15: học viên 12 tuổi chờ đồng ý, chưa có email; ghi đồng ý thì có email mời', async () => {
  await createUser(admin, {
    role: 'STUDENT',
    name: `Bé Thử ${RUN}`,
    email: CHILD_EMAIL,
    birthDate: birthDateYearsAgo(12),
  })
  const users = (await apiCall(adminToken, '/users?role=STUDENT')).body
  const child = users.find((u: { email: string }) => u.email === CHILD_EMAIL)
  expect(child.status).toBe('AWAITING_GUARDIAN_CONSENT')
  await admin.waitForTimeout(1000)
  expect(await mailsTo(CHILD_EMAIL)).toHaveLength(0)

  await admin.goto(`/admin/users`)
  await admin.getByPlaceholder('Tìm theo tên hoặc email…').fill(CHILD_EMAIL)
  await admin
    .getByRole('row')
    .filter({ hasText: CHILD_EMAIL })
    .getByRole('button', { name: 'Người giám hộ' })
    .click()
  const form = dialog(admin)
  await form.getByPlaceholder('Ví dụ: Hoàng Thị Lan').fill('Mẹ Của Bé')
  await form.getByPlaceholder('0912888999 hoặc lan.hoang@email.com').fill('0912888999')
  await form.getByRole('button', { name: 'Xác nhận đồng ý' }).click()

  await expect(async () => {
    expect((await mailsTo(CHILD_EMAIL)).length).toBeGreaterThan(0)
  }).toPass({ timeout: 20_000 })
})

test('bước 16: chưa tích đồng ý thì bị từ chối; tích đồng ý thì đăng nhập được', async ({
  browser,
}) => {
  const page = await acceptInvite(browser, CHILD_EMAIL)
  await page.getByRole('button', { name: 'Kích hoạt tài khoản' }).click()
  await expect(page).toHaveURL(/accept-invite/)
  await expect(page.getByText(/đồng ý/i).first()).toBeVisible()

  await page.getByRole('checkbox').check()
  await page.getByRole('button', { name: 'Kích hoạt tài khoản' }).click()
  await expect(page.getByRole('heading', { name: 'Kích hoạt thành công' })).toBeVisible()
  await page.context().close()

  const student = await login(browser, CHILD_EMAIL)
  await expect(student).toHaveURL(/\/student$/)
  await student.context().close()
})

test('bước 17: còn dưới 12 giờ thì không đổi lịch được, hủy có cảnh báo bị tính buổi', async ({
  browser,
}) => {
  const token = await apiToken('student1@demo.local')
  const booked = (await apiCall(token, '/sessions?status=BOOKED')).body
  const upcoming = booked
    .filter((s: { startsAt: string }) => new Date(s.startsAt) > new Date())
    .sort((a: { startsAt: string }, b: { startsAt: string }) =>
      a.startsAt.localeCompare(b.startsAt),
    )[0]
  expect(upcoming, 'seed có buổi sắp tới của student1').toBeTruthy()
  moveSession(upcoming.id, '6 hours', '7 hours')

  const student = await login(browser, 'student1@demo.local')
  await student.goto(`/sessions/${upcoming.id}`)
  await expect(student.getByText(/Đã quá hạn đổi lịch/)).toBeVisible()
  await expect(student.getByRole('button', { name: 'Đổi lịch' })).toBeHidden()

  const blocked = await apiCall(token, `/sessions/${upcoming.id}/reschedule`, 'POST', {
    newStartsAt: new Date(Date.now() + 5 * 86_400_000).toISOString(),
  })
  expect(blocked.status).toBe(409)
  expect(blocked.body.code).toBe('CANCEL_WINDOW_PASSED')

  await student.getByRole('button', { name: /Hủy lịch buổi học/ }).click()
  await expect(student.getByText(/bị tính trừ 1 buổi/)).toBeVisible()
  await student.getByRole('button', { name: 'Xác nhận hủy' }).click()
  await expect(student.getByText('Hủy muộn', { exact: true })).toBeVisible()
  const cancelled = (await apiCall(token, `/sessions/${upcoming.id}`)).body
  expect(cancelled.status).toBe('CANCELLED_LATE')
  await student.context().close()
})
