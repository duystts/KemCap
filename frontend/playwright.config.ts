import { defineConfig } from '@playwright/test'

/**
 * Smoke test demo (T142). Cần sẵn backend profile `local,demo-seed`, frontend, Postgres và Mailpit;
 * xem quickstart.md mục 3. Các bước nối tiếp nhau và làm đổi dữ liệu seed nên chạy trên database mới.
 */
export default defineConfig({
  testDir: './e2e',
  workers: 1,
  fullyParallel: false,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: [['list']],
  outputDir: './test-results',
  use: {
    baseURL: process.env.E2E_APP_URL ?? 'http://localhost:5173',
    channel: 'chrome',
    viewport: { width: 1280, height: 900 },
    actionTimeout: 15_000,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
})
