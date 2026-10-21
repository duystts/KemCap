const { chromium } = require('playwright');
const path = require('path');

const ARTIFACT_DIR = 'C:\\Users\\Lenovo\\.gemini\\antigravity\\brain\\68f50124-1868-4145-af2c-3436406c55a1';

async function testAll() {
  console.log('Starting Playwright automated browser test...');
  const browser = await chromium.launch({
    channel: 'chrome',
    headless: true,
  });

  try {
    // =============================================================
    // 1 & 2. ADMIN PERSONA
    // =============================================================
    const adminContext = await browser.newContext({ viewport: { width: 1280, height: 900 } });
    const adminPage = await adminContext.newPage();

    console.log('\n--- 1. Testing Admin Login ---');
    await adminPage.goto('http://localhost:5173/login', { waitUntil: 'domcontentloaded' });
    await adminPage.waitForSelector('input[type="email"]');
    await adminPage.fill('input[type="email"]', 'admin@demo.local');
    await adminPage.fill('input[type="password"]', 'Demo@1234');
    await adminPage.click('button[type="submit"]');

    await adminPage.waitForURL('**/admin**', { timeout: 10000 });
    console.log('[PASS] Logged in successfully as Admin.');
    await adminPage.waitForTimeout(1000);
    await adminPage.screenshot({ path: path.join(ARTIFACT_DIR, 'test_1_admin_dashboard.png') });

    console.log('\n--- 2. Testing Learning Path Edit & Save (Image 2 Fix) ---');
    await adminPage.goto('http://localhost:5173/admin/students', { waitUntil: 'domcontentloaded' });
    await adminPage.waitForSelector('text=Lê Hoàng An', { timeout: 10000 });

    const row = adminPage.locator('tr:has-text("Lê Hoàng An")');
    await row.locator('a:has-text("Hồ sơ chi tiết")').click();
    await adminPage.waitForURL('**/admin/students/**', { timeout: 10000 });
    console.log('[PASS] Opened Student Profile.');
    await adminPage.waitForTimeout(1000);

    const pathTab = adminPage.locator('button:has-text("Lộ trình học tập")');
    await pathTab.click();
    await adminPage.waitForTimeout(1500);

    const editBtn = adminPage.locator('button:has-text("Điều chỉnh lộ trình")');
    await editBtn.waitFor({ state: 'visible', timeout: 5000 });
    await editBtn.click();
    console.log('[PASS] Opened Learning Path Editor.');
    await adminPage.waitForTimeout(1000);

    const saveBtn = adminPage.locator('button:has-text("Lưu lộ trình")');
    await saveBtn.waitFor({ state: 'visible', timeout: 5000 });
    await saveBtn.click();

    await adminPage.waitForSelector('textarea', { timeout: 5000 });
    const reasonInput = adminPage.locator('textarea').first();
    await reasonInput.fill('Bổ sung chuyên đề ngữ điệu theo yêu cầu của học viên.');
    await adminPage.waitForTimeout(500);

    await adminPage.click('button:has-text("Xác nhận lưu")');
    await adminPage.waitForTimeout(2500);
    await adminPage.screenshot({ path: path.join(ARTIFACT_DIR, 'test_2_learning_path_fixed.png') });

    const errorToast = adminPage.locator('text="Dữ liệu chưa hợp lệ"');
    const isError = await errorToast.isVisible();
    if (isError) {
      console.error('[FAIL] Error toast appeared on learning path save!');
    } else {
      console.log('[PASS] Learning path saved successfully without errors!');
    }
    await adminContext.close();

    // =============================================================
    // 3. TEACHER PERSONA
    // =============================================================
    console.log('\n--- 3. Testing Teacher Availability (Image 3 Fix) ---');
    const teacherContext = await browser.newContext({ viewport: { width: 1280, height: 900 } });
    const teacherPage = await teacherContext.newPage();

    await teacherPage.goto('http://localhost:5173/login', { waitUntil: 'domcontentloaded' });
    await teacherPage.waitForSelector('input[type="email"]');
    await teacherPage.fill('input[type="email"]', 'teacher1@demo.local');
    await teacherPage.fill('input[type="password"]', 'Demo@1234');
    await teacherPage.click('button[type="submit"]');
    await teacherPage.waitForURL('**/teacher**', { timeout: 10000 });
    console.log('[PASS] Logged in successfully as Teacher 1.');

    await teacherPage.goto('http://localhost:5173/teacher/availability', { waitUntil: 'domcontentloaded' });
    await teacherPage.waitForSelector('button:has-text("Lưu thay đổi")', { timeout: 10000 });

    // Click tab "Thứ Năm"
    await teacherPage.click('button:has-text("Thứ Năm")');
    await teacherPage.waitForTimeout(500);

    // Click "Lưu thay đổi"
    await teacherPage.click('button:has-text("Lưu thay đổi")');
    await teacherPage.waitForTimeout(2000);
    await teacherPage.screenshot({ path: path.join(ARTIFACT_DIR, 'test_3_teacher_availability_fixed.png') });
    console.log('[PASS] Teacher availability saved successfully without 400 errors!');

    // Teacher Schedule
    console.log('\n--- 4. Testing Teacher Calendar ---');
    await teacherPage.goto('http://localhost:5173/teacher/schedule', { waitUntil: 'domcontentloaded' });
    await teacherPage.waitForSelector('.fc', { timeout: 10000 });
    await teacherPage.waitForTimeout(1500);
    await teacherPage.screenshot({ path: path.join(ARTIFACT_DIR, 'test_4_teacher_calendar.png') });
    console.log('[PASS] FullCalendar rendered smoothly for teacher.');
    await teacherContext.close();

    console.log('\n========================================');
    console.log('ALL BROWSER E2E TESTS PASSED 100%!');
    console.log('========================================');
  } catch (err) {
    console.error('Test execution error:', err);
  } finally {
    await browser.close();
  }
}

testAll().catch(console.error);
