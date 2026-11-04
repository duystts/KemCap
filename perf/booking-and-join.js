// T141: kiểm NFR-PERF-01/02, NFR-SCAL-01. Cách chạy xem perf/README.md.
// - booking: 20 người dùng ảo xem slot 30 ngày rồi đặt buổi
// - join: 400 lượt vào phòng trong 60 giây
// - read: 200 người dùng ảo đọc lịch, phiếu, thông báo trong 10 phút
import http from 'k6/http'
import { check, sleep } from 'k6'
import exec from 'k6/execution'
import { Counter } from 'k6/metrics'

const API = (__ENV.BASE_URL || 'http://localhost:8080') + '/api/v1'
const PASSWORD = 'Perf@1234'
const STUDENTS = 1000
const TEACHERS = 50
const TOKEN_MINUTES = 12

const unexpected = new Counter('unexpected_status')
const conflicts = new Counter('booking_conflicts')

// 409 khi đặt là kết quả nghiệp vụ hợp lệ: slot vừa bị người khác lấy
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 299 }, 409))

const limits = ['p(95)<500', 'p(99)<1000']

export const options = {
  setupTimeout: '5m',
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    booking: {
      executor: 'constant-vus',
      exec: 'booking',
      vus: 20,
      duration: '3m',
    },
    join: {
      executor: 'constant-arrival-rate',
      exec: 'joinRoom',
      startTime: '3m',
      rate: 400,
      timeUnit: '60s',
      duration: '60s',
      preAllocatedVUs: 50,
      maxVUs: 100,
    },
    read: {
      executor: 'ramping-vus',
      exec: 'read',
      startTime: '3m',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 200 },
        { duration: '10m', target: 200 },
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    'http_req_duration{name:slots}': limits,
    'http_req_duration{name:book}': limits,
    'http_req_duration{name:join}': limits,
    'http_req_duration{name:read}': limits,
    'http_req_failed{name:slots}': ['rate==0'],
    'http_req_failed{name:book}': ['rate==0'],
    'http_req_failed{name:join}': ['rate==0'],
    'http_req_failed{name:read}': ['rate==0'],
    unexpected_status: ['count==0'],
  },
}

function login(email) {
  const res = http.post(`${API}/auth/login`, JSON.stringify({ email, password: PASSWORD }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  })
  if (res.status !== 200) {
    unexpected.add(1, { name: 'login', status: String(res.status) })
    throw new Error(`Đăng nhập ${email} trả ${res.status}`)
  }
  return res.json('accessToken')
}

function auth(token, extra) {
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, ...extra }
}

function expectStatus(res, name, ...allowed) {
  if (!check(res, { [`${name} đúng mã`]: (r) => allowed.includes(r.status) })) {
    unexpected.add(1, { name, status: String(res.status) })
    console.error(`${name} ${res.request.method} ${res.url} trả ${res.status}: ${String(res.body).slice(0, 160)}`)
  }
}

/** Token sống 15 phút; đăng nhập lại trước khi hết hạn. Lượt đăng nhập không tính vào ngưỡng. */
const tokens = {}
function tokenFor(email) {
  // k6 tái dùng người dùng ảo giữa các kịch bản, nên token phải gắn với email chứ không với VU
  const cached = tokens[email]
  if (cached && Date.now() - cached.at <= TOKEN_MINUTES * 60_000) return cached.token
  const token = login(email)
  tokens[email] = { token, at: Date.now() }
  return token
}

/** Mỗi cặp giáo viên, học viên có một buổi đang mở phòng; token cấp sẵn để không tính bcrypt. */
export function setup() {
  const rooms = []
  for (let t = 1; t <= TEACHERS; t++) {
    const teacherToken = login(`perf-t${t}@test.local`)
    const studentToken = login(`perf-s${t}@test.local`)
    const now = new Date()
    const until = new Date(now.getTime() + 15 * 60_000)
    const res = http.get(
      `${API}/sessions?from=${now.toISOString()}&to=${until.toISOString()}&status=BOOKED`,
      auth(teacherToken, { tags: { name: 'setup' } }),
    )
    const open = res.json().filter((s) => new Date(s.startsAt) > now)
    if (!open.length) throw new Error(`Giáo viên ${t} không có phòng mở: chạy lại seed.sql`)
    rooms.push({ sessionId: open[0].id, token: teacherToken })
    rooms.push({ sessionId: open[0].id, token: studentToken })
  }
  return { rooms }
}

export function booking() {
  // Mỗi người dùng ảo giữ một học viên riêng, nằm ngoài nhóm giữ phòng mở
  const n = TEACHERS + ((__VU - 1) % (STUDENTS - TEACHERS)) + 1
  const t = tokenFor(`perf-s${n}@test.local`)
  if (!__ITER) {
    // Lấy id ghi danh một lần cho mỗi người dùng ảo
    const list = http.get(`${API}/enrollments`, auth(t, { tags: { name: 'setup' } })).json()
    enrollmentId = list[0].id
  }
  const from = new Date()
  const to = new Date(from.getTime() + 30 * 864e5)
  const slots = http.get(
    `${API}/enrollments/${enrollmentId}/slots?from=${from.toISOString()}&to=${to.toISOString()}`,
    auth(t, { tags: { name: 'slots' } }),
  )
  expectStatus(slots, 'slots', 200)
  const list = slots.status === 200 ? slots.json() : []
  sleep(1)
  if (list.length) {
    const slot = list[Math.floor(Math.random() * list.length)]
    const res = http.post(
      `${API}/sessions`,
      JSON.stringify({ enrollmentId, startsAt: slot.startsAt }),
      auth(t, { tags: { name: 'book' } }),
    )
    expectStatus(res, 'book', 201, 409)
    if (res.status === 409) conflicts.add(1)
  }
  sleep(1)
}
let enrollmentId = null

export function joinRoom(data) {
  // __ITER tính riêng theo từng VU; iterationInTest mới rải đều qua mọi phòng
  const room = data.rooms[exec.scenario.iterationInTest % data.rooms.length]
  const res = http.post(
    `${API}/sessions/${room.sessionId}/join`,
    null,
    auth(room.token, { tags: { name: 'join' } }),
  )
  expectStatus(res, 'join', 200)
}

export function read() {
  const teacher = __VU % 4 === 0
  const email = teacher
    ? `perf-t${(__VU % TEACHERS) + 1}@test.local`
    : `perf-s${(__VU % STUDENTS) + 1}@test.local`
  const t = tokenFor(email)
  const tagged = auth(t, { tags: { name: 'read' } })
  const from = new Date()
  const to = new Date(from.getTime() + 7 * 864e5)
  expectStatus(
    http.get(`${API}/sessions?from=${from.toISOString()}&to=${to.toISOString()}`, tagged),
    'read',
    200,
  )
  sleep(1 + Math.random() * 2)
  expectStatus(http.get(`${API}/notifications`, tagged), 'read', 200)
  sleep(1 + Math.random() * 2)
  expectStatus(
    http.get(teacher ? `${API}/session-reports?status=PENDING` : `${API}/assignments`, tagged),
    'read',
    200,
  )
  sleep(1 + Math.random() * 2)
}
