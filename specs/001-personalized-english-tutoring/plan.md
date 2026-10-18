# Implementation Plan: Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (MVP)

**Branch**: `001-personalized-english-tutoring` | **Date**: 2026-10-12 · cập nhật 2026-10-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-personalized-english-tutoring/spec.md`

## Summary

Xây dựng bản demo MVP trong 4 tuần (hạn 2026-11-12) cho nền tảng dạy tiếng Anh 1 kèm 1: giáo vụ
quản lý học viên, giáo viên, gói học; học viên đặt lịch theo giờ rảnh của giáo viên và học qua phòng
video tích hợp trên web; giáo viên đánh giá sau mỗi buổi, xây lộ trình cá nhân; hệ thống tự trừ buổi,
nhắc lịch, theo dõi học phí và tiến bộ. Phạm vi demo theo mục "Phạm vi bản demo MVP" trong spec.

**Cập nhật 2026-10-18** (spec Clarifications Session 2026-10-18 + [aidlc/nfr.md](./aidlc/nfr.md) §4):
quy tắc kích hoạt gói, chốt kết quả buổi, hủy/đổi muộn, trạng thái `CANCELLED_SYSTEM`; tuân thủ dữ
liệu cá nhân (đồng ý chính sách, đồng ý người giám hộ cho học viên < 16 tuổi, yêu cầu xem/xóa và ẩn
danh hóa — FR-030, FR-031, R16); sao lưu (R17); bổ sung mục tiêu hiệu năng, k6, gitleaks. Cắt giảm đã duyệt tại cổng units (K1–K3): báo cáo chất
lượng FR-023, upload tệp/`FileStorage`/S3, rate limit đăng nhập, log JSON/runbook → GĐ2.

Hướng kỹ thuật: **Modular Monolith** Spring Boot + Spring Modulith (6 module) cung cấp REST API cho
SPA React. Chống trùng lịch bằng exclusion constraint của PostgreSQL; module giao tiếp qua event được
lưu bền (Event Publication Registry); mọi dịch vụ bên ngoài (LiveKit, SMTP; S3/R2 từ GĐ2) sau port có adapter
`fake`. Toàn bộ chạy với chi phí 0 đồng (xem [research.md](./research.md)).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5 (frontend)

**Primary Dependencies**: Spring Boot 4.1.x; Spring Modulith, Spring Security (OAuth2 Resource
Server, JWT), Spring Data JPA (phiên bản Spring theo BOM của Boot 4.1), Flyway, springdoc-openapi, ShedLock, LiveKit Java server SDK, AWS SDK v2
(S3 — GĐ2, K2), Spring Mail + Thymeleaf (email template) · React 19, Vite, Tailwind CSS, shadcn/ui, React Router,
TanStack Query, react-hook-form + zod, FullCalendar, Recharts, `@livekit/components-react`, openapi-typescript

**Storage**: PostgreSQL 16+ (extension `btree_gist`); file trên S3-compatible (MinIO local, Cloudflare R2) — **GĐ2 (K2)**; MVP chỉ nộp bài văn bản, sao lưu `pg_dump` vẫn ghi lên R2 (R17)

**Testing**: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), Spring Modulith test · Vitest +
Testing Library · Playwright (smoke) · **k6** (tải, tuần 4) · **gitleaks** (quét secret trên PR) ·
GitHub Actions CI; Maven Enforcer cấm `h2` (test DB luôn là PostgreSQL thật)

**Target Platform**: Backend Linux container (Oracle Cloud Always Free VM, ARM); frontend tĩnh trên
Cloudflare Pages; trình duyệt Chrome/Edge/Safari/Firefox bản mới, desktop và mobile web

**Project Type**: Web application (backend REST API + frontend SPA)

**Performance Goals**: API p95 < 500 ms, p99 < 1 s trên dữ liệu quy mô thiết kế (1.000 HV, 50 GV); đặt
lịch < 1 phút thao tác (SC-001); phiếu đánh giá < 3 phút (SC-008); job nhắc lịch trễ ≤ 2 phút so với
mốc 24h/1h; email `SENT` ≤ 5 phút sau mốc nhắc; nhắc phiếu quá hạn / buổi chưa chốt ≤ 60 phút; LCP
mobile ≤ 3 s (Should) — chi tiết [aidlc/nfr.md](./aidlc/nfr.md)

**Constraints**: chi phí 0 đồng; backend ≤ 1 GB RAM; 4 người × 4 tuần; video không đi qua backend;
UI tiếng Việt; thời gian lưu UTC, hiển thị theo múi giờ người xem

**Scale/Scope**: demo ~10 giáo viên, ~50 học viên; thiết kế cho ~1.000 học viên; khoảng 33 màn hình
(thêm màn hình đồng ý, đồng ý người giám hộ, yêu cầu dữ liệu + trang tóm tắt); SC-010 (200 buổi đồng
thời) đo sau ra mắt — demo chỉ kiểm mức backend bằng k6 (R14, spec đã chốt)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Kiểm tra | Trước research | Sau design |
|---|---|---|---|
| I. Modular Monolith | 6 module đúng constitution; mỗi module chỉ lộ public API + event ([events-and-ports.md](./contracts/events-and-ports.md)); bảng không JOIN chéo module; `ApplicationModules.verify()` trong CI | ✅ | ✅ — thêm module `shared` (open module) chỉ chứa hạ tầng chung (security, lỗi, `FileStorage`), không chứa nghiệp vụ |
| II. Provider-Agnostic | Video, email, file sau port; adapter `fake` cho mỗi port; nghiệp vụ không import SDK | ✅ | ✅ — `PaymentGateway` và AI chưa tạo (YAGNI), đã ghi chỗ sẽ gắn vào |
| III. Free-First & YAGNI | Không RabbitMQ/Kafka/Redis/microservice; mọi dịch vụ có gói free hoặc tự host | ✅ | ✅ — job dùng `@Scheduled` + ShedLock; event dùng Modulith registry |
| IV. Test nghiệp vụ trọng yếu | Test bắt buộc: đặt lịch, chống trùng (đồng thời), hủy/đổi (kể cả đổi muộn bị chặn), trừ/hoàn buổi (kể cả `CANCELLED_SYSTEM`, chốt thay, Gián đoạn), hết hạn/gia hạn gói, học phí/công nợ, phân quyền, ẩn danh hóa giữ nguyên số liệu (SC-007); Testcontainers; CI trên mỗi PR | ✅ | ✅ — liệt kê trong quickstart §3–4 (bước 1–24) |
| V. Bảo mật & dữ liệu | Phân quyền ở backend + kiểm tra phân công; BCrypt; secret qua env; xóa mềm; Flyway; đồng ý xử lý dữ liệu + đồng ý người giám hộ (FR-030); yêu cầu xem/xóa → ẩn danh hóa (FR-031) | ✅ | ✅ — trả 404 khi không có quyền; rate limit đăng nhập/quên mật khẩu (NFR-SEC-06) hoãn sang GĐ2 theo K3; app không khởi động khi JWT secret < 256 bit (NFR-SEC-05); mỗi module tự ẩn danh hóa dữ liệu của mình qua event (R16); mốc 16 tuổi + nội dung chính sách chờ pháp lý trung tâm |
| Ràng buộc công nghệ (v1.0.1) | Spring Boot 4.1.x; thư viện Spring theo BOM; không dùng phiên bản hết hỗ trợ OSS (R1) | ✅ | ✅ — thư viện bên thứ ba được kiểm tra tương thích Boot 4.1 ở Sprint 0 ngày 1 |

**Kết luận gate**: PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-personalized-english-tutoring/
├── plan.md              # File này
├── research.md          # Phase 0: quyết định kỹ thuật
├── data-model.md        # Phase 1: bảng, trạng thái, ràng buộc
├── quickstart.md        # Phase 1: chạy local + kịch bản kiểm chứng
├── contracts/
│   ├── rest-api.md          # REST API v1 theo module
│   └── events-and-ports.md  # event giữa module + provider ports
├── checklists/requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
backend/
├── pom.xml
└── src/
    ├── main/java/com/englishplatform/
    │   ├── EnglishPlatformApplication.java
    │   ├── shared/              # open module: security config, JWT, ProblemDetail handler,
    │   │                        #   clock, paging ([GĐ2 — K2]: FileStorage port + adapters s3, fake)
    │   ├── identity/            # UserDirectory, UserDeactivationGuard (SPI), events | internal/: entity, repo, service, web,
    │   │                        #   privacy/ (policy consent, guardian consent, data subject request, anonymize), ratelimit/
    │   ├── catalog/             # EnrollmentApi, events  | internal/: profiles, course, package, enrollment
    │   ├── scheduling/          # VideoRoomProvider, events | internal/: availability, slots, session, jobs, livekit adapter
    │   ├── learning/            # events | internal/: assessment, goal, path, report, assignment, feedback, progress
    │   ├── billing/             # events | internal/: receivable, payment, revenue report
    │   └── notification/        # EmailSender | internal/: listeners, templates, dispatch, in-app notifications
    ├── main/resources/
    │   ├── application.yml, application-local.yml, application-demo-seed.yml
    │   ├── db/migration/        # V1__shared_infra.sql, V2__identity_….sql, …
    │   └── templates/email/     # Thymeleaf email templates (tiếng Việt)
    └── test/java/com/englishplatform/
        ├── ModularityTests.java # ApplicationModules.verify() + sinh tài liệu module
        ├── support/             # Testcontainers base, test data builders
        └── <module>/            # unit + @ApplicationModuleTest integration tests

frontend/
├── package.json, vite.config.ts, tailwind.config.ts
└── src/
    ├── app/                 # router, layout theo vai trò, auth guard, query client
    ├── lib/api/             # fetch client (refresh token), schema.d.ts sinh từ OpenAPI
    ├── components/ui/       # shadcn/ui + component dùng chung (DataTable, DateTimeTZ, EmptyState…)
    └── features/
        ├── auth/            # login, accept invite (+ đồng ý chính sách), consent gate, forgot/reset password
        ├── admin/           # dashboard, users, reports, privacy/ (yêu cầu dữ liệu, đồng ý người giám hộ, trang tóm tắt in được)
        ├── catalog/         # courses, packages, enrollments, profiles
        ├── scheduling/      # availability editor, slot picker, calendar, session detail
        ├── classroom/       # LiveKit room page
        ├── learning/        # proficiency, goals, path editor, session report, assignments, progress
        ├── billing/         # payments, receivables
        └── notifications/   # bell + list
    tests/ (vitest) · e2e/ (playwright)

docker-compose.yml           # postgres, mailpit, livekit (dev); minio từ GĐ2 (K2)
deploy/                      # docker-compose.prod.yml (+ backup sidecar pg_dump → R2), Caddyfile, .env.example, runbook.md
perf/                        # kịch bản k6 (tuần 4)
.github/workflows/ci.yml     # backend verify + frontend lint/test/build trên mỗi PR
```

**Structure Decision**: Monorepo gồm `backend/` (một ứng dụng Spring Boot, mỗi module là một package
cấp cao; class trong `internal/` không được module khác truy cập) và `frontend/` (SPA chia theo
`features/` tương ứng module backend). Cấu trúc này khớp với việc phân công theo module bên dưới.

## Phân công module (đầu vào cho /speckit-tasks)

| Người | Sở hữu | Ghi chú |
|---|---|---|
| **#1 Leader** | **Toàn bộ backend**: `shared`, `identity` (gồm privacy), `catalog`, `scheduling` (gồm LiveKit adapter), `learning`, `billing`, `notification`; hạ tầng (repo, CI, docker-compose, deploy, sao lưu, k6); OpenAPI là hợp đồng cho frontend | quyết định 2026-10-18; backend là đường găng duy nhất — thứ tự và phần cắt giảm xem [aidlc/unit-of-work.md](./aidlc/unit-of-work.md) |
| **#2** | Frontend `features/scheduling`, `features/classroom` (LiveKit UI) | làm trên mock sinh từ OpenAPI / adapter `fake` cho tới khi API thật xong |
| **#3** | Frontend `features/learning`; ngoại lệ D1: backend U4 (năng lực, mục tiêu, lộ trình), #1 review | nhiều màn hình nhất (lộ trình, phiếu đánh giá, tiến bộ) |
| **#4** | Frontend `features/catalog`, `features/billing`, `features/admin` (dashboard, báo cáo, quản lý người dùng, `admin/privacy`); ngoại lệ D1: backend billing core (U2) + U8, #1 review | |
| **#2** (thêm) | Frontend app shell (router, layout, API client, auth, consent gate), `features/auth` | chốt tại cổng units 2026-10-18 |
| **#3** (thêm) | Chuông thông báo `features/notifications` | |

**Phụ thuộc chính**: frontend #2–#4 → backend #1 qua OpenAPI (`/v3/api-docs`, sinh type bằng
`openapi-typescript`) · thứ tự backend theo DAG unit. Giảm chặn: #1 công bố contract (DTO + endpoint
rỗng trả dữ liệu mẫu) của mỗi module trước khi cài thật, frontend làm trên mock.

## Rủi ro & giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Một người (#1) làm toàn bộ backend → đường găng duy nhất, nghẽn khi #1 bận/ốm | Thứ tự backend theo DAG unit; contract-first để FE không chờ; cắt giảm đã nêu trong unit-of-work.md; tuần 4 không thêm tính năng |
| Phòng video LiveKit tốn thời gian hơn dự kiến | #1 (token API) + #2 (UI) làm spike ngày 2–3 với `livekit-server --dev`; nếu không ổn tới cuối tuần 2 → tạm dùng component dựng sẵn tối thiểu, bỏ tùy biến giao diện |
| 4 tuần không đủ | Thứ tự ưu tiên theo spec (P1 → P3); các mục "rút gọn" có thể cắt thêm; tuần 4 dành cho ổn định, không thêm tính năng |
| Lệch API giữa FE/BE | Sinh type từ OpenAPI; contracts là thỏa thuận ban đầu, thay đổi phải báo owner |
| Hạn mức free thay đổi | Mọi provider có adapter `fake`; demo vẫn chạy được local nếu dịch vụ free có vấn đề |
| Hosting free ngủ → mất nhắc lịch | Dùng VM luôn chạy (R13), không dùng Render/Koyeb free cho backend |
| Quota LiveKit free không đủ (~54.000 participant-phút/tháng ước tính ở quy mô demo) | Theo dõi mức dùng; > 80% quota → tự host LiveKit hoặc nâng gói (cần ghi Complexity Tracking) |
| Một VM chứa cả backend và DB | Sao lưu hằng ngày ra R2 + diễn tập khôi phục (R17); fallback Neon/Supabase |
| Oracle thu hồi VM Always Free nhàn rỗi | ⚠ verify chính sách; healthcheck ping định kỳ; sao lưu R17 |
| Tuân thủ dữ liệu cá nhân (đồng ý, trẻ em, dữ liệu ở nhà cung cấp nước ngoài) | FR-030/FR-031 trong MVP (R16); pháp lý trung tâm xác nhận mốc tuổi + nội dung chính sách trước khi dùng dữ liệu thật |
| Phạm vi tăng sau clarify (privacy, hủy hệ thống, chốt thay) | Privacy giao #1 tuần 2–3; nếu trễ, cắt thêm mục "Rút gọn" theo spec chứ không cắt FR-030/FR-031 (constitution V là MUST) |

## Complexity Tracking

Không có vi phạm cần biện minh.

**Ngưỡng kích hoạt** (khi xảy ra phải thêm dòng biện minh vào bảng này): tự host LiveKit khi dùng > 80%
quota free; cache phân tán cho rate limit khi chạy > 1 instance; hoãn FR-030/FR-031 khỏi MVP (vi
phạm constitution V — không được phép nếu chưa sửa constitution).
