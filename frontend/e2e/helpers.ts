import { execSync } from 'node:child_process'
import { expect, type Browser, type Page } from '@playwright/test'

const API = process.env.E2E_API_URL ?? 'http://localhost:8080/api/v1'
const MAILPIT = process.env.E2E_MAILPIT_URL ?? 'http://localhost:8025'
const REPO_ROOT = new URL('../..', import.meta.url)

export const PASSWORD = 'Demo@1234'

/** Mỗi lần chạy một hậu tố để email mới không trùng. */
export const RUN = String(Date.now()).slice(-6)

export async function login(browser: Browser, email: string, password = PASSWORD): Promise<Page> {
  const context = await browser.newContext()
  const page = await context.newPage()
  await page.goto('/login')
  await page.fill('#email', email)
  await page.fill('#password', password)
  await page.click('button[type=submit]')
  await page.waitForURL((url) => !url.pathname.startsWith('/login'))
  return page
}

export async function apiToken(email: string, password = PASSWORD): Promise<string> {
  const response = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  expect(response.status, `đăng nhập API ${email}`).toBe(200)
  return (await response.json()).accessToken
}

export async function apiCall(
  token: string,
  path: string,
  method = 'GET',
  body?: unknown,
): Promise<{ status: number; body: any }> {
  const response = await fetch(API + path, {
    method,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await response.text()
  return { status: response.status, body: text ? JSON.parse(text) : null }
}

/** Dời giờ buổi học để tạo tình huống theo thời gian; backend giữ đồng hồ thật nên không giả lập được. */
export function moveSession(sessionId: string, startsAt: string, endsAt: string) {
  const sql = `update session set starts_at = now() + interval '${startsAt}', ends_at = now() + interval '${endsAt}' where id = '${sessionId}'`
  const psql =
    process.env.E2E_PSQL ?? 'docker compose exec -T postgres psql -U app -d english -At -c'
  execSync(`${psql} "${sql}"`, { cwd: REPO_ROOT, stdio: 'pipe' })
}

interface MailSummary {
  ID: string
  Subject: string
}

export async function mailsTo(address: string): Promise<MailSummary[]> {
  const url = `${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:${address}`)}`
  return (await (await fetch(url)).json()).messages ?? []
}

/** Lấy link kích hoạt trong email mời. Email gửi bất đồng bộ nên chờ tới khi có. */
export async function inviteLink(address: string): Promise<string> {
  let link = ''
  await expect(async () => {
    const [message] = await mailsTo(address)
    expect(message, `email mời tới ${address}`).toBeTruthy()
    const detail = await (await fetch(`${MAILPIT}/api/v1/message/${message.ID}`)).json()
    const match = `${detail.Text ?? ''} ${detail.HTML ?? ''}`.match(
      /https?:\/\/[^\s"'<>]+\/accept-invite\?token=[^\s"'<>]+/,
    )
    expect(match, 'link kích hoạt trong email').toBeTruthy()
    link = match![0].replace(/&amp;/g, '&')
  }).toPass({ timeout: 20_000 })
  return link
}

export async function acceptInvite(browser: Browser, address: string): Promise<Page> {
  const page = await (await browser.newContext()).newPage()
  await page.goto(await inviteLink(address))
  await page.fill('#password', PASSWORD)
  await page.fill('#confirm', PASSWORD)
  return page
}

/** Ngày dạng YYYY-MM-DD theo giờ máy, cách hôm nay `years` năm. */
export function birthDateYearsAgo(years: number) {
  const d = new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear() - years}-${month}-${day}`
}

/** Dialog của app là khối `.fixed` phủ màn hình. */
export const dialog = (page: Page) => page.locator('.fixed').last()

export async function createUser(
  page: Page,
  user: { role: 'STUDENT' | 'TEACHER'; name: string; email: string; birthDate?: string },
) {
  await page.goto('/admin/users')
  await page.getByRole('button', { name: 'Thêm người dùng' }).click()
  const form = dialog(page)
  await form.locator('select').first().selectOption(user.role)
  await form.getByPlaceholder('Ví dụ: Nguyễn Văn An').fill(user.name)
  await form.getByPlaceholder('an.nguyen@example.com').fill(user.email)
  if (user.birthDate) await form.locator('input[type=date]').fill(user.birthDate)
  await form.getByRole('button', { name: 'Tạo người dùng' }).click()
  await expect(form.getByRole('button', { name: 'Tạo người dùng' })).toBeHidden()
}

/** Chọn ngày đầu tiên có khung giờ trống; trả về giờ bắt đầu của các khung giờ ngày đó. */
export async function openFirstDayWithSlots(page: Page): Promise<string[]> {
  const days = page.locator('button').filter({ hasText: /\d{2}[/-]\d{2}$/ })
  await days.first().waitFor()
  const count = await days.count()
  for (let i = 0; i < count; i++) {
    await days.nth(i).click()
    await page.waitForTimeout(800)
    const slots = page.getByTestId('slot-item')
    if ((await slots.count()) > 0) {
      const texts = await slots.allInnerTexts()
      return texts.map((t) => t.match(/\d{2}:\d{2}/)![0])
    }
  }
  throw new Error('Không có khung giờ trống nào trong 14 ngày tới')
}
