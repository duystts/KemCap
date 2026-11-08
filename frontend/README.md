# Frontend

React 19 + Vite + TypeScript + Tailwind 4, TanStack Query, React Router 7. Chạy ở `http://localhost:5173`.

## Yêu cầu

- Node.js 22 LTS.
- Backend đang chạy ở `http://localhost:8080` (xem [backend/README.md](../backend/README.md)), trừ khi dùng mock.
- Chrome đã cài, nếu chạy e2e.

## Chạy local

```bash
cd frontend
npm install
cp .env.example .env.local   # tùy chọn: giá trị mặc định đã trỏ vào backend local
npm run dev                  # http://localhost:5173
```

Đăng nhập bằng tài khoản demo (mật khẩu `Demo@1234`): `admin@demo.local` (giáo vụ), `teacher1@demo.local`,
`student1@demo.local`… Tài khoản chỉ có khi backend chạy với profile `demo-seed`.

Phải mở đúng `http://localhost:5173`: backend chỉ cho phép CORS từ `FRONTEND_URL` (mặc định địa chỉ này). Chạy
ở port khác thì đặt `FRONTEND_URL` cho backend tương ứng.

### Chạy không cần backend (mock)

```bash
VITE_USE_MOCKS=true npm run dev
```

Dữ liệu giả lập bằng MSW theo `contracts/rest-api.md` (`src/mocks/handlers/`). Chỉ để dựng giao diện; mock có
thể lệch backend thật, nên mọi thay đổi vẫn phải kiểm lại với backend.

## Biến môi trường

Đặt trong `.env.local` (không commit) hoặc trước lệnh.

| Biến | Mặc định | Ghi chú |
|---|---|---|
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Không có dấu `/` ở cuối |
| `VITE_USE_MOCKS` | `false` | `true` = dùng mock MSW thay cho backend |

## Lệnh

| Lệnh | Tác dụng |
|---|---|
| `npm run dev` | Chạy dev server có hot reload |
| `npm run build` | Kiểm kiểu (`tsc -b`) rồi build ra `dist/` |
| `npm run preview` | Chạy thử bản build |
| `npm run lint` | oxlint |
| `npm run format` | Prettier sửa định dạng `src/` |
| `npm run test -- --run` | Unit test (Vitest), chạy một lần |
| `npm run gen:api` | Sinh type từ OpenAPI của backend đang chạy (profile `local`) vào `src/lib/api/schema.d.ts` |
| `npm run e2e` | Smoke test Playwright theo kịch bản demo |

Trước khi mở PR, chạy giống CI:

```bash
npx tsc -b && npx prettier --check src && npm run lint && npm run test -- --run && npm run build
```

## Smoke test e2e

Cần cả hệ thống đang chạy: PostgreSQL, Mailpit, backend (`local,demo-seed`) và `npm run dev`. Các bước nối tiếp
nhau và dùng hết dữ liệu seed, nên mỗi lần chạy cần **database mới** (xem "Làm mới dữ liệu demo" trong
[backend/README.md](../backend/README.md)).

```bash
npm run e2e
```

Test dời giờ buổi học bằng `docker compose exec postgres psql`, nên mặc định cần hạ tầng chạy bằng
`docker-compose.yml` ở thư mục gốc. Khác cấu hình mặc định thì đặt biến:

| Biến | Mặc định |
|---|---|
| `E2E_APP_URL` | `http://localhost:5173` |
| `E2E_API_URL` | `http://localhost:8080/api/v1` |
| `E2E_MAILPIT_URL` | `http://localhost:8025` |
| `E2E_PSQL` | `docker compose exec -T postgres psql -U app -d english -At -c` |

Ví dụ hạ tầng chạy dưới compose project khác tên (`docker compose -p demo up -d`):

```bash
E2E_PSQL='docker compose -p demo exec -T postgres psql -U app -d english -At -c' npm run e2e
```

Khi lỗi, ảnh chụp và trace nằm trong `test-results/`; xem trace bằng `npx playwright show-trace <đường dẫn trace.zip>`.

## Cấu trúc

| Thư mục | Nội dung |
|---|---|
| `src/app/` | Router, layout, đăng nhập, phân quyền theo vai trò |
| `src/features/` | Màn hình theo module backend: `catalog`, `scheduling`, `learning`, `billing`, `admin`… |
| `src/lib/api/` | Hàm gọi API và type; `client.ts` xử lý token và lỗi RFC 9457 |
| `src/components/` | Thành phần dùng chung |
| `src/mocks/` | Handler MSW cho chế độ mock |
| `e2e/` | Smoke test Playwright |

Giao diện theo style đã duyệt: nền ngà, nhấn màu đất nung, tiêu đề serif, nút bằng chữ, không icon trang trí.
Màu được định nghĩa lại trong `@theme` (`src/index.css`), nên các lớp `slate`, `indigo`… đã mang bảng màu này.

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Đăng nhập báo lỗi mạng hoặc CORS | Backend chưa chạy, hoặc frontend không chạy ở `FRONTEND_URL` của backend |
| `npm run gen:api` lỗi | Backend phải đang chạy với profile `local` (mới có `/v3/api-docs`) |
| e2e lỗi `service "postgres" is not running` | Hạ tầng chạy dưới compose project khác: đặt `E2E_PSQL` như trên |
| e2e lỗi "cần chạy trên database mới" | Làm mới dữ liệu demo rồi chạy lại |
