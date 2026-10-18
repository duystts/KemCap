# User Stories & Personas — Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (001-personalized-english-tutoring)

**Nguồn**: spec.md (User Scenarios, Functional Requirements, Key Entities, Clarifications — gồm Session 2026-10-12 và Session 2026-10-18) và `.specify/memory/constitution.md` (v1.0.1). File này BỔ SUNG chứ không thay thế spec.md. Nếu có mâu thuẫn, spec.md và constitution được ưu tiên.

**Cập nhật lần cuối**: 2026-10-18 — đồng bộ với clarify Session 2026-10-18 (thêm FR-030, FR-031; chốt V-2, V-3, V-6..V-22).

**Quy ước**:
- ID story: `S-US<x>.<n>` — `x` là số User Story trong spec, `n` là thứ tự story con.
- Ưu tiên: kế thừa ưu tiên của User Story trong spec (P1/P2/P3). Nhãn **[Demo: Đầy đủ]**, **[Demo: Rút gọn]**, **[GĐ2]** theo bảng "Phạm vi bản demo MVP" của spec.
- Story hoặc acceptance gắn **[GĐ2]** được ghi lại để giữ truy vết, KHÔNG làm và KHÔNG kiểm trong MVP/demo (constitution III — YAGNI; spec Clarifications 2026-10-18: "acceptance của demo chỉ kiểm phần MVP").
- Phân quyền trong mọi story MUST được kiểm tra ở backend (constitution V); "chỉ thấy/không thấy" trong acceptance nghĩa là API cũng từ chối, không chỉ giao diện ẩn.
- Thời gian lưu UTC, hiển thị theo múi giờ người xem (constitution — Ràng buộc công nghệ; spec Edge Cases).
- Ký hiệu *(đã chốt V-x)*: acceptance đã khớp quyết định trong spec Clarifications; *(giả định)*: chưa có trong spec, cần xác nhận.

---

## Personas

### P1 — Giáo vụ (Quản trị viên / Admin)
- **Mục tiêu**: quản lý tập trung học viên, giáo viên, khóa học, gói học; xếp giáo viên phụ trách; đặt/đổi/hủy lịch thay học viên; ghi nhận học phí và theo dõi công nợ; xem báo cáo chất lượng và tài chính; ghi nhận đồng ý của người giám hộ và xử lý yêu cầu của chủ thể dữ liệu.
- **Nỗi đau hiện tại**: xếp lịch và nhắc lịch thủ công tốn thời gian (SC-002 kỳ vọng giảm ≥ 70%); khó đối chiếu số buổi còn lại và công nợ; không biết giáo viên nào chậm nộp đánh giá hay có tỷ lệ hủy/vắng cao.
- **Tần suất dùng / thiết bị**: hằng ngày, chủ yếu máy tính (trình duyệt); có thể dùng điện thoại qua trình duyệt.
- **Quyền truy cập dữ liệu** (theo constitution V, FR-001):
  - Đọc/ghi: hồ sơ học viên, giáo viên (tạo, sửa, ngừng kích hoạt, kích hoạt lại — không xóa cứng, FR-004); trình độ sơ bộ và mục tiêu khi tạo hồ sơ, sửa mục tiêu (FR-016); khóa học, gói học; ghi danh, học phí của ghi danh (sửa khi giảm giá), phân công giáo viên; lịch và buổi học của mọi học viên, chốt kết quả buổi thay giáo viên (FR-014), quyết định buổi Gián đoạn; thanh toán, công nợ, gia hạn thủ công; cấu hình hạn chót hủy (mốc nhắc lịch cố định 24h/1h, không cấu hình — FR-013); đồng ý của cha mẹ/người giám hộ (FR-030); nhật ký yêu cầu của chủ thể dữ liệu, ẩn danh hóa, xuất bản tóm tắt dữ liệu (FR-031).
  - Đọc: hồ sơ năng lực, lộ trình, phiếu đánh giá, bài tập, trang tiến bộ của mọi học viên (FR-021); đánh giá sao từng buổi của học viên (FR-022 — chỉ giáo vụ); báo cáo chất lượng và tài chính.
  - Không được: xem mật khẩu (chỉ lưu dạng băm); xóa cứng hồ sơ. Ngoại lệ duy nhất là ẩn danh hóa theo yêu cầu xóa hợp lệ (FR-004, FR-031).

### P2 — Giáo viên (Teacher)
- **Mục tiêu**: khai báo lịch rảnh; dạy buổi học 1 kèm 1 ngay trên website; đánh dấu kết quả buổi; ghi kết quả kiểm tra đầu vào chính thức, xây dựng và điều chỉnh lộ trình; nộp phiếu đánh giá sau buổi nhanh (SC-008: < 3 phút); giao và chấm bài tập.
- **Nỗi đau hiện tại**: lịch rảnh/lịch dạy rời rạc; phải dùng ứng dụng video khác; không có chỗ lưu tiến bộ có cấu trúc của từng học viên; dễ quên viết nhận xét sau buổi.
- **Tần suất dùng / thiết bị**: nhiều lần mỗi ngày dạy; máy tính có webcam/micro để vào phòng học; điện thoại để xem lịch, nhận nhắc.
- **Quyền truy cập dữ liệu** (constitution V, FR-001, FR-003):
  - Đọc/ghi: lịch rảnh và ngày nghỉ của chính mình; kết quả buổi học của chính mình (Hoàn thành / Học viên vắng / Gián đoạn); hồ sơ năng lực, mục tiêu, lộ trình, phiếu đánh giá, bài tập và chấm bài — CHỈ của học viên được phân công.
  - Đọc: thông tin học viên được phân công, buổi học của chính mình, trang tiến bộ học viên được phân công.
  - Không được: xem học viên không được phân công; xem/sửa thanh toán, công nợ, báo cáo tài chính; tạo/sửa tài khoản; sửa dữ liệu giáo viên khác; xem đánh giá sao/nhận xét từng buổi của học viên trong MVP (FR-022, *đã chốt V-12*). Dạy thay thuộc [GĐ2] nên MVP không có quyền "giáo viên dạy thay" (*đã chốt V-7*).

### P3 — Học viên (Student, gồm cả trường hợp phụ huynh dùng chung tài khoản)
- **Mục tiêu**: đặt lịch nhanh (SC-001: < 1 phút); vào lớp đúng giờ không cần cài ứng dụng; hiểu rõ mình đang ở đâu trên lộ trình và tiến bộ thế nào (SC-009); xem số buổi đã học/đã đặt/còn lại; nộp bài tập; đánh giá buổi học.
- **Nỗi đau hiện tại**: không biết lộ trình và tiến bộ của bản thân; quên lịch học dẫn đến bị trừ buổi; không rõ còn bao nhiêu buổi, gói hết hạn khi nào.
- **Tần suất dùng / thiết bị**: 1–vài lần mỗi tuần; máy tính hoặc điện thoại qua trình duyệt (ứng dụng di động ngoài phạm vi MVP).
- **Quyền truy cập dữ liệu** (constitution V, FR-001):
  - Đọc: CHỈ dữ liệu của chính mình — hồ sơ, hồ sơ năng lực, mục tiêu, lộ trình, nhận xét/phiếu đánh giá buổi, bài tập và điểm, số buổi đã học/đã đặt/còn lại, hạn gói, lịch sử thanh toán và công nợ của mình (chỉ đọc — FR-025, *đã chốt V-13*).
  - Ghi: đồng ý điều khoản & chính sách dữ liệu (FR-030); đặt/đổi/hủy buổi của mình theo chính sách; nộp bài tập; đánh giá sao buổi đã hoàn thành (trong 7 ngày, không sửa sau khi gửi).
  - Không được: xem dữ liệu học viên khác; xem lịch rảnh của giáo viên không phụ trách; sửa hồ sơ năng lực/lộ trình/mục tiêu; sửa thanh toán/công nợ; tự tạo tài khoản (FR-002 — giáo vụ tạo hoặc mời).
- **Ghi chú phụ huynh**: không có tài khoản/persona riêng (Clarifications). Phụ huynh dùng chung tài khoản học viên nhỏ tuổi nên có cùng quyền với học viên. Với học viên dưới 16 tuổi, đồng ý của cha mẹ/người giám hộ do giáo vụ ghi nhận trước khi tài khoản được kích hoạt (FR-030, *đã chốt V-14*); yêu cầu xóa/xem dữ liệu gửi qua kênh ngoài cho giáo vụ (FR-031, *đã chốt V-15*).

### Tác nhân hệ thống (không phải persona)
- **Hệ thống (tác vụ tự động)**: gửi nhắc lịch, nhắc chốt kết quả buổi, nhắc nộp phiếu, nhắc gia hạn; tự cập nhật số buổi; chuyển gói sang hết hạn; tự hủy buổi tương lai khi đổi giáo viên / ngừng kích hoạt học viên; yêu cầu đồng ý lại khi chính sách đổi phiên bản. Được dùng làm chủ ngữ trong một số acceptance criteria.

---

## Story map

**Tổng số**: 52 story (49 trong MVP, 3 gắn [GĐ2]: S-US3.7, S-US4.7, S-US6.4). US1: 15 · US2: 12 · US3: 7 · US4: 7 · US5: 7 · US6: 4.

| Epic / User story spec | Story con (INVEST) | Persona | Ưu tiên | Unit (điền sau) |
|------------------------|--------------------|---------|---------|-----------------|
| US1 — Quản lý tập trung (P1) | S-US1.1 Đăng nhập email/mật khẩu | P1, P2, P3 | P1 | |
| | S-US1.2 Quên / đặt lại mật khẩu | P1, P2, P3 | P1 | |
| | S-US1.3 Giáo vụ tạo / mời tài khoản giáo viên, học viên | P1 | P1 | |
| | S-US1.4 Quản lý hồ sơ học viên (tạo, sửa, ngừng / kích hoạt lại) | P1 | P1 | |
| | S-US1.5 Quản lý hồ sơ giáo viên (tạo, sửa, ngừng / kích hoạt lại) | P1 | P1 | |
| | S-US1.6 Định nghĩa khóa học và gói học | P1 | P1 | |
| | S-US1.7 Ghi danh học viên vào gói và phân công giáo viên | P1 | P1 | |
| | S-US1.8 Đổi giáo viên phụ trách không mất dữ liệu | P1 | P1 | |
| | S-US1.9 Tìm kiếm và lọc học viên / giáo viên / buổi học [Demo: Rút gọn] | P1 | P1 | |
| | S-US1.10 Phân quyền theo vai trò và phạm vi dữ liệu | P1, P2, P3 | P1 | |
| | S-US1.11 Đồng ý điều khoản & chính sách dữ liệu (lần đầu và khi đổi phiên bản) | P1, P2, P3 | P1 | |
| | S-US1.12 Ghi nhận đồng ý của cha mẹ/người giám hộ cho học viên dưới 16 tuổi | P1 | P1 | |
| | S-US1.13 Ghi nhận yêu cầu của chủ thể dữ liệu (xóa / xem) | P1 | P1 | |
| | S-US1.14 Ẩn danh hóa hồ sơ học viên theo yêu cầu xóa hợp lệ | P1 | P1 | |
| | S-US1.15 Xuất bản tóm tắt dữ liệu cá nhân theo yêu cầu xem | P1 | P1 | |
| US2 — Đặt lịch & lớp trực tuyến (P1) | S-US2.1 Khai báo lịch rảnh định kỳ hằng tuần | P2 | P1 | |
| | S-US2.2 Khai báo ngày nghỉ ngoại lệ | P2 | P1 | |
| | S-US2.3 Học viên đặt buổi học trong khung trống | P3 | P1 | |
| | S-US2.4 Chống trùng lịch và đặt đồng thời | P2, P3 | P1 | |
| | S-US2.5 Giáo vụ đặt / đổi / hủy thay học viên | P1 | P1 | |
| | S-US2.6 Học viên hủy / đổi lịch theo hạn chót | P3 | P1 | |
| | S-US2.7 Hạn chót hủy theo cấu hình triển khai (mốc nhắc lịch cố định) | P1 | P1 | |
| | S-US2.8 Thông báo khi đặt / đổi / hủy lịch | P2, P3 | P1 | |
| | S-US2.9 Nhắc lịch 24 giờ và 1 giờ trước buổi | P2, P3 | P1 | |
| | S-US2.10 Vào phòng học trực tuyến tích hợp | P2, P3 | P1 | |
| | S-US2.11 Ghi nhận trạng thái buổi học | P2, P1 | P1 | |
| | S-US2.12 Hiển thị lịch theo múi giờ người xem | P1, P2, P3 | P1 | |
| US3 — Hồ sơ năng lực & lộ trình (P2) | S-US3.1 Ghi kết quả kiểm tra đầu vào theo kỹ năng | P2 | P2 | |
| | S-US3.2 Ghi và sửa mục tiêu học tập | P2, P1 | P2 | |
| | S-US3.3 Đánh giá định kỳ và lịch sử hồ sơ năng lực | P2 | P2 | |
| | S-US3.4 Giáo viên tự soạn lộ trình cá nhân [Demo: Rút gọn] | P2 | P2 | |
| | S-US3.5 Học viên xem mục tiêu, trình độ và lộ trình | P3 | P2 | |
| | S-US3.6 Điều chỉnh lộ trình kèm lý do và lưu lịch sử | P2 | P2 | |
| | S-US3.7 Khởi tạo lộ trình từ mẫu do giáo vụ quản lý [GĐ2] | P1, P2 | GĐ2 | |
| US4 — Đánh giá sau buổi & tiến bộ (P2) | S-US4.1 Giáo viên nộp phiếu đánh giá buổi học | P2 | P2 | |
| | S-US4.2 Nhắc phiếu quá hạn 24 giờ và báo cho giáo vụ | P2, P1 | P2 | |
| | S-US4.3 Học viên xem nhận xét và bài tập từng buổi | P3 | P2 | |
| | S-US4.4 Học viên nộp bài tập (văn bản / tệp, gồm tệp âm thanh) [Demo: Rút gọn] | P3 | P2 | |
| | S-US4.5 Giáo viên chấm và phản hồi bài tập | P2 | P2 | |
| | S-US4.6 Trang tiến bộ [Demo: Rút gọn] | P3, P2, P1 | P2 | |
| | S-US4.7 Ghi âm bài nộp trực tiếp trên web [GĐ2] | P3 | GĐ2 | |
| US5 — Số buổi & học phí (P3) | S-US5.1 Ghi nhận thanh toán và kích hoạt gói | P1 | P3 | |
| | S-US5.2 Xem công nợ và lịch sử thanh toán | P1, P3 | P3 | |
| | S-US5.3 Tự động cập nhật số buổi đã học / đã đặt / còn lại | Hệ thống, P1, P3 | P3 | |
| | S-US5.4 Chặn đặt lịch khi gói hết buổi hoặc hết hạn | P3, P1 | P3 | |
| | S-US5.5 Nhắc gia hạn gói | P3, P1 | P3 | |
| | S-US5.6 Gia hạn thủ công ghi danh đã hết hạn | P1 | P3 | |
| | S-US5.7 Báo cáo tổng hợp tài chính và số buổi theo giáo viên [Demo: Rút gọn] | P1 | P3 | |
| US6 — Chất lượng giảng dạy (P3) | S-US6.1 Học viên đánh giá buổi học (1–5 sao) | P3 | P3 | |
| | S-US6.2 Báo cáo chất lượng theo giáo viên [Demo: Rút gọn] | P1 | P3 | |
| | S-US6.3 Xem chi tiết từng buổi từ báo cáo chất lượng | P1 | P3 | |
| | S-US6.4 Chỉ số tỷ lệ đúng giờ và tiến bộ trung bình [GĐ2] | P1 | GĐ2 | |

---

## Stories chi tiết

### US1 — Quản lý tập trung học viên, giáo viên và gói học (P1)

#### S-US1.1 — Đăng nhập bằng email và mật khẩu
**As a** người dùng (giáo vụ, giáo viên, học viên) **I want** đăng nhập bằng email và mật khẩu **so that** tôi truy cập đúng chức năng của vai trò mình.

**Acceptance (Given/When/Then)**:
1. Given tài khoản đang hoạt động, When nhập đúng email và mật khẩu, Then đăng nhập thành công và vào màn hình theo vai trò.
2. Given nhập sai mật khẩu, When gửi đăng nhập, Then bị từ chối với thông báo chung (không tiết lộ email có tồn tại hay không).
3. Given tài khoản đã bị ngừng kích hoạt, When đăng nhập, Then bị từ chối.
4. Given mật khẩu được lưu, When kiểm tra dữ liệu lưu trữ, Then mật khẩu chỉ ở dạng băm (BCrypt hoặc tương đương).

**Truy vết**: FR-001, FR-002, US1 (constitution V)

#### S-US1.2 — Quên / đặt lại mật khẩu
**As a** người dùng **I want** đặt lại mật khẩu khi quên **so that** tôi không phải nhờ giáo vụ cấp lại.

**Acceptance (Given/When/Then)**:
1. Given tài khoản đang hoạt động, When yêu cầu quên mật khẩu với email của mình, Then nhận email chứa liên kết đặt lại mật khẩu.
2. Given liên kết đặt lại còn hiệu lực, When đặt mật khẩu mới, Then đăng nhập được bằng mật khẩu mới và liên kết không dùng lại được.
3. Given email không tồn tại, When yêu cầu quên mật khẩu, Then hệ thống phản hồi giống trường hợp có tồn tại.

**Truy vết**: FR-002, US1

#### S-US1.3 — Giáo vụ tạo / mời tài khoản giáo viên và học viên
**As a** giáo vụ **I want** tạo hoặc mời tài khoản cho giáo viên và học viên **so that** chỉ người được trung tâm chấp nhận mới dùng được hệ thống.

**Acceptance (Given/When/Then)**:
1. Given giáo vụ đã đăng nhập, When tạo tài khoản với email và vai trò Giáo viên hoặc Học viên, Then người được mời nhận email để kích hoạt và đặt mật khẩu (kèm bước đồng ý điều khoản — S-US1.11).
2. Given email đã được dùng cho tài khoản khác, When tạo tài khoản, Then hệ thống từ chối và báo trùng.
3. Given một người không có lời mời, When cố tự đăng ký, Then không có chức năng tự đăng ký.
4. Given học viên dưới 16 tuổi (theo ngày sinh) chưa có đồng ý của cha mẹ/người giám hộ, When giáo vụ cố gửi lời mời/kích hoạt tài khoản, Then hệ thống chặn và yêu cầu ghi nhận đồng ý trước (S-US1.12). *(đã chốt V-14)*

**Truy vết**: FR-002, FR-004, FR-030, US1, Edge Case "Học viên dưới 16 tuổi chưa có đồng ý"

#### S-US1.4 — Quản lý hồ sơ học viên
**As a** giáo vụ **I want** tạo, sửa, ngừng kích hoạt và kích hoạt lại hồ sơ học viên (họ tên, ngày sinh, liên hệ, múi giờ, trình độ sơ bộ tự khai, mục tiêu) **so that** dữ liệu học viên được quản lý tập trung.

**Acceptance (Given/When/Then)**:
1. Given giáo vụ đã đăng nhập, When tạo hồ sơ học viên với họ tên, ngày sinh, liên hệ, trình độ sơ bộ (tự khai) và mục tiêu, Then hồ sơ được lưu và xuất hiện trong danh sách học viên có thể tìm kiếm/lọc. *(đã chốt V-11 — trình độ sơ bộ sẽ được thay bằng kết quả kiểm tra đầu vào chính thức, S-US3.1)*
2. Given hồ sơ đã tồn tại, When giáo vụ sửa thông tin, Then thay đổi được lưu.
3. Given học viên nghỉ học và còn buổi Đã đặt trong tương lai, When giáo vụ ngừng kích hoạt hồ sơ, Then hồ sơ chuyển sang ngừng kích hoạt (không xóa vĩnh viễn), lịch sử học tập vẫn còn, và các buổi Đã đặt tương lai bị hủy, không trừ buổi. *(đã chốt V-16)*
4. Given hồ sơ đã ngừng kích hoạt (không phải do ẩn danh hóa), When giáo vụ kích hoạt lại, Then học viên dùng lại được tài khoản. *(đã chốt V-16)*
5. Given hồ sơ đã bị ẩn danh hóa, When giáo vụ cố kích hoạt lại, Then hệ thống từ chối (ngừng kích hoạt vĩnh viễn — S-US1.14). *(đã chốt V-15)*

**Truy vết**: FR-004, FR-015, FR-016, FR-031, US1 (Acceptance 1)

#### S-US1.5 — Quản lý hồ sơ giáo viên
**As a** giáo vụ **I want** tạo, sửa, ngừng kích hoạt và kích hoạt lại hồ sơ giáo viên (chuyên môn, chứng chỉ) **so that** tôi biết năng lực từng giáo viên khi phân công.

**Acceptance (Given/When/Then)**:
1. Given giáo vụ đã đăng nhập, When tạo hồ sơ giáo viên với chuyên môn và chứng chỉ, Then hồ sơ được lưu và xuất hiện trong danh sách giáo viên.
2. Given giáo viên nghỉ việc và không còn học viên được phân công, When giáo vụ ngừng kích hoạt hồ sơ, Then hồ sơ không bị xóa vĩnh viễn và giáo viên không đăng nhập được.
3. Given giáo viên còn học viên được phân công, When giáo vụ ngừng kích hoạt, Then hệ thống chặn và liệt kê các học viên cần đổi giáo viên phụ trách trước (S-US1.8). *(đã chốt V-16)*
4. Given hồ sơ giáo viên đã ngừng kích hoạt, When giáo vụ kích hoạt lại, Then giáo viên đăng nhập lại được. *(đã chốt V-16)*

**Truy vết**: FR-004, FR-006, US1

#### S-US1.6 — Định nghĩa khóa học và gói học
**As a** giáo vụ **I want** định nghĩa khóa học (ví dụ Giao tiếp, IELTS, Tiếng Anh thiếu nhi) và gói học (số buổi, thời lượng mỗi buổi, thời hạn sử dụng, giá) **so that** tôi có thể ghi danh học viên.

**Acceptance (Given/When/Then)**:
1. Given giáo vụ đã đăng nhập, When tạo gói 24 buổi, 45 phút/buổi, thời hạn và giá, Then gói được lưu và chọn được khi ghi danh.
2. Given thiếu số buổi, thời lượng, thời hạn hoặc giá, When lưu gói, Then hệ thống báo lỗi trường bắt buộc.
3. Given gói đã có học viên ghi danh, When giáo vụ sửa giá gói, Then học phí của các ghi danh đã có không đổi (học phí được sao từ giá gói lúc ghi danh). *(đã chốt V-13)*

**Truy vết**: FR-005, FR-025, US1

#### S-US1.7 — Ghi danh học viên vào gói và phân công giáo viên
**As a** giáo vụ **I want** ghi danh học viên vào một gói học và phân công giáo viên phụ trách **so that** học viên có buổi để đặt lịch và giáo viên biết mình phụ trách ai.

**Acceptance (Given/When/Then)**:
1. Given đã có học viên, giáo viên và gói học, When giáo vụ ghi danh học viên vào gói 24 buổi và phân công giáo viên, Then ghi danh ở trạng thái "Chờ thanh toán", học phí của ghi danh được sao từ giá gói, và giáo viên thấy học viên trong danh sách của mình.
2. Given ghi danh vừa tạo, When xem chi tiết ghi danh, Then thấy trạng thái, số buổi đã học/đã đặt/còn lại; ngày kích hoạt và ngày hết hạn để trống cho tới khi có thanh toán đầu tiên.
3. Given ghi danh "Chờ thanh toán", When học viên cố đặt lịch, Then hệ thống chặn và báo gói chưa kích hoạt; sau khi giáo vụ ghi nhận khoản thanh toán đầu tiên, học viên có 24 buổi khả dụng. *(đã chốt V-1, clarify 2026-10-18)*

**Truy vết**: FR-006, FR-024, FR-025, US1 (Acceptance 2)

#### S-US1.8 — Đổi giáo viên phụ trách không mất dữ liệu
**As a** giáo vụ **I want** đổi giáo viên phụ trách của một học viên **so that** khi giáo viên nghỉ việc, học viên vẫn học tiếp mà không mất lịch sử.

**Acceptance (Given/When/Then)**:
1. Given học viên có lịch sử học tập, đánh giá và lộ trình với giáo viên A, When giáo vụ đổi sang giáo viên B, Then toàn bộ lịch sử, đánh giá và lộ trình được giữ nguyên và giáo viên B xem được.
2. Given đã đổi sang giáo viên B, When giáo viên A mở danh sách học viên, Then không còn thấy học viên đó và API từ chối truy cập dữ liệu của học viên.
3. Given học viên còn buổi Đã đặt với giáo viên A trong tương lai, When đổi giáo viên, Then các buổi đó tự động bị hủy, không trừ buổi (trả lại chỗ), không tính vào tỷ lệ hủy của giáo viên A; học viên, giáo viên A và giáo viên B được thông báo; học viên đặt lại với giáo viên B. *(đã chốt V-8)*

**Truy vết**: FR-006, FR-003, FR-023, FR-024, US1 (Edge Case "Giáo viên nghỉ việc/đổi giáo viên phụ trách")

#### S-US1.9 — Tìm kiếm và lọc học viên / giáo viên / buổi học [Demo: Rút gọn]
**As a** giáo vụ **I want** tìm theo tên và lọc theo trạng thái, giáo viên **so that** tôi nhanh chóng tìm được hồ sơ hoặc buổi học cần xử lý.

**Acceptance (Given/When/Then)**:
1. Given có nhiều học viên, When tìm theo một phần họ tên, Then danh sách chỉ gồm học viên khớp tên.
2. Given có nhiều học viên/buổi học, When lọc theo trạng thái hoặc theo giáo viên, Then danh sách chỉ gồm bản ghi khớp bộ lọc.
3. Given giáo viên đăng nhập, When tìm kiếm học viên, Then kết quả chỉ trong phạm vi học viên được phân công.
4. *(GĐ sau — FR-007 đầy đủ)* Lọc theo khóa học và khoảng thời gian chưa làm trong demo.

**Truy vết**: FR-007, FR-003, US1 (Acceptance 1)

#### S-US1.10 — Phân quyền theo vai trò và phạm vi dữ liệu
**As a** giáo vụ (chủ sở hữu dữ liệu trung tâm) **I want** mỗi vai trò chỉ xem/sửa dữ liệu trong phạm vi của mình **so that** dữ liệu cá nhân học viên được bảo vệ theo pháp luật.

**Acceptance (Given/When/Then)**:
1. Given giáo viên đăng nhập, When mở danh sách học viên, Then chỉ thấy học viên được phân công cho mình.
2. Given giáo viên gọi trực tiếp API lấy hồ sơ/lộ trình/kết quả của học viên không được phân công, When gửi yêu cầu, Then backend từ chối (không phụ thuộc giao diện).
3. Given học viên đăng nhập, When cố truy cập dữ liệu của học viên khác, Then backend từ chối.
4. Given giáo viên hoặc học viên, When cố truy cập chức năng giáo vụ (tạo tài khoản, ghi nhận thanh toán, báo cáo, đồng ý người giám hộ, yêu cầu chủ thể dữ liệu), Then backend từ chối.
5. Given giáo viên, When gọi API lấy đánh giá sao/nhận xét từng buổi của học viên (kể cả về chính mình), Then backend từ chối. *(đã chốt V-12)*
6. Given học viên, When mở lịch sử thanh toán và công nợ của chính mình, Then xem được ở chế độ chỉ đọc; mọi thao tác ghi bị backend từ chối. *(đã chốt V-13)*

**Truy vết**: FR-001, FR-003, FR-022, FR-025, US1 (Acceptance 3) (constitution V)

#### S-US1.11 — Đồng ý điều khoản & chính sách dữ liệu (lần đầu và khi đổi phiên bản) [Demo: Đầy đủ]
**As a** người dùng (giáo vụ, giáo viên, học viên) **I want** đọc và đồng ý điều khoản sử dụng và chính sách xử lý dữ liệu cá nhân khi đặt mật khẩu lần đầu, và đồng ý lại khi chính sách đổi **so that** trung tâm xử lý dữ liệu của tôi có căn cứ pháp lý (constitution V).

**Acceptance (Given/When/Then)**:
1. Given người dùng mở liên kết kích hoạt và đặt mật khẩu lần đầu, When chưa tích đồng ý điều khoản & chính sách dữ liệu, Then không hoàn tất được việc đặt mật khẩu/kích hoạt.
2. Given người dùng tích đồng ý và đặt mật khẩu, When hoàn tất, Then hệ thống lưu bản ghi đồng ý gồm người đồng ý, phiên bản chính sách và thời điểm đồng ý (UTC).
3. Given chính sách đã đổi sang phiên bản mới và người dùng mới chỉ đồng ý phiên bản cũ, When người dùng đăng nhập, Then được yêu cầu đồng ý phiên bản mới; khi đồng ý, bản ghi mới (phiên bản + thời điểm) được lưu và bản ghi cũ vẫn giữ.
4. Given người dùng đã đồng ý phiên bản hiện hành, When đăng nhập, Then không bị hỏi lại.
5. Given người dùng chưa đồng ý lại phiên bản mới, When gọi API nghiệp vụ, Then backend chặn và giao diện chỉ hiện màn hình đồng ý cho tới khi đồng ý. *(đã chốt V-25)*

**Truy vết**: FR-030, FR-002, US1; Clarifications 2026-10-18 (Q đồng ý xử lý dữ liệu). *(đã chốt V-14)* — nội dung điều khoản cần pháp lý trung tâm xác nhận.

#### S-US1.12 — Ghi nhận đồng ý của cha mẹ/người giám hộ cho học viên dưới 16 tuổi [Demo: Đầy đủ]
**As a** giáo vụ **I want** ghi nhận đồng ý của cha mẹ/người giám hộ cho học viên dưới 16 tuổi trước khi kích hoạt tài khoản **so that** việc xử lý dữ liệu cá nhân trẻ em đúng pháp luật Việt Nam.

**Acceptance (Given/When/Then)**:
1. Given hồ sơ học viên có ngày sinh cho thấy dưới 16 tuổi tại thời điểm kích hoạt, When giáo vụ lưu hồ sơ, Then hồ sơ được tạo nhưng tài khoản ở trạng thái chưa kích hoạt (không gửi lời mời, không đăng nhập được) và hiển thị "Chờ đồng ý của người giám hộ".
2. Given học viên dưới 16 tuổi chưa có đồng ý, When giáo vụ ghi nhận đồng ý với họ tên người giám hộ, quan hệ, liên hệ và ngày đồng ý, Then bản ghi đồng ý được lưu kèm phiên bản chính sách, người ghi nhận và thời điểm ghi nhận, và tài khoản học viên được phép kích hoạt (gửi lời mời).
3. Given thiếu họ tên, quan hệ, liên hệ hoặc ngày đồng ý, When lưu, Then báo lỗi trường bắt buộc.
4. Given học viên từ 16 tuổi trở lên, When tạo hồ sơ, Then không yêu cầu đồng ý của người giám hộ; chỉ cần đồng ý của chính học viên (S-US1.11).
5. Given giáo viên hoặc học viên, When cố xem/ghi đồng ý của người giám hộ, Then backend từ chối.

**Truy vết**: FR-030, FR-004, US1; Edge Case "Học viên dưới 16 tuổi chưa có đồng ý"; Key Entity Consent. *(đã chốt V-14)* — mốc 16 tuổi cần pháp lý xác nhận; trường hợp thiếu ngày sinh — xem #V-24.

#### S-US1.13 — Ghi nhận yêu cầu của chủ thể dữ liệu (xóa / xem) [Demo: Đầy đủ]
**As a** giáo vụ **I want** ghi nhận mỗi yêu cầu xóa hoặc xem dữ liệu cá nhân nhận được qua kênh ngoài **so that** trung tâm có nhật ký xử lý yêu cầu theo pháp luật.

**Acceptance (Given/When/Then)**:
1. Given giáo vụ nhận yêu cầu qua email/điện thoại, When tạo bản ghi yêu cầu với học viên liên quan, người yêu cầu, ngày nhận và loại (xóa hoặc xem), Then yêu cầu được lưu ở trạng thái chưa xử lý.
2. Given yêu cầu chưa xử lý, When giáo vụ xử lý xong (ẩn danh hóa — S-US1.14, xuất tóm tắt — S-US1.15, hoặc từ chối vì không hợp lệ), Then bản ghi lưu ngày xử lý, kết quả và người xử lý.
3. Given có nhiều yêu cầu, When giáo vụ mở danh sách, Then thấy các yêu cầu theo trạng thái và ngày nhận.
4. Given giáo viên hoặc học viên, When cố xem/tạo yêu cầu, Then backend từ chối.

**Truy vết**: FR-031, US1; Key Entity Data subject request. *(đã chốt V-15)*

#### S-US1.14 — Ẩn danh hóa hồ sơ học viên theo yêu cầu xóa hợp lệ [Demo: Đầy đủ]
**As a** giáo vụ **I want** ẩn danh hóa hồ sơ học viên khi có yêu cầu xóa hợp lệ **so that** trung tâm tuân thủ quyền xóa dữ liệu mà số liệu tài chính và thù lao vẫn khớp (SC-007).

**Acceptance (Given/When/Then)**:
1. Given học viên còn buổi Đã đặt trong tương lai, When giáo vụ ẩn danh hóa, Then các buổi đó chuyển Hủy hệ thống (không trừ buổi, không tính tỷ lệ hủy). *(đã chốt V-23/V-26)*
2. Given yêu cầu xóa đã được ghi nhận, When giáo vụ xác nhận ẩn danh hóa (có cảnh báo không khôi phục được), Then họ tên, liên hệ, ngày sinh, đồng ý của người giám hộ, hồ sơ năng lực, mục tiêu, lộ trình, điểm kỹ năng và nhận xét phiếu đánh giá, bài tập/bài nộp (nội dung, tệp, điểm, phản hồi) và nhận xét kèm đánh giá sao bị xóa không khôi phục được; bản ghi yêu cầu xóa được giữ. *(đã chốt V-26)*
3. Given hồ sơ vừa ẩn danh hóa, When giáo vụ xem báo cáo tài chính (S-US5.7) và số buổi, Then số buổi, lịch sử buổi, thanh toán, công nợ và đánh giá sao vẫn còn ở dạng không định danh và tổng số liệu không thay đổi so với trước khi ẩn danh hóa.
4. Given học viên còn công nợ, When ẩn danh hóa, Then công nợ vẫn được giữ ở dạng không định danh.
5. Given hồ sơ đã ẩn danh hóa, When học viên cố đăng nhập hoặc giáo vụ cố kích hoạt lại, Then bị từ chối (ngừng kích hoạt vĩnh viễn).
6. Given ẩn danh hóa hoàn tất, When xem yêu cầu tương ứng (S-US1.13), Then yêu cầu được đánh dấu đã xử lý với ngày xử lý, kết quả, người xử lý.

**Truy vết**: FR-031, FR-004, US1; Edge Case "Yêu cầu xóa dữ liệu của học viên còn buổi Đã đặt hoặc còn công nợ"; SC-007. *(đã chốt V-15)* — phạm vi dữ liệu học tập còn lại sau ẩn danh hóa: xem #V-26.

#### S-US1.15 — Xuất bản tóm tắt dữ liệu cá nhân theo yêu cầu xem [Demo: Đầy đủ]
**As a** giáo vụ **I want** xuất bản tóm tắt dữ liệu cá nhân đang lưu về một học viên **so that** đáp ứng yêu cầu xem dữ liệu của học viên/phụ huynh.

**Acceptance (Given/When/Then)**:
1. Given yêu cầu xem đã được ghi nhận cho học viên X, When giáo vụ chọn xuất bản tóm tắt, Then nhận được một tệp/bản tóm tắt gồm dữ liệu cá nhân đang lưu về học viên X (thông tin cá nhân, liên hệ, đồng ý, ghi danh, buổi học, thanh toán/công nợ, hồ sơ năng lực, mục tiêu, lộ trình, nhận xét, bài tập).
2. Given bản tóm tắt của học viên X, When kiểm tra nội dung, Then không chứa dữ liệu cá nhân của học viên khác.
3. Given xuất xong, When xem yêu cầu tương ứng, Then giáo vụ đánh dấu được đã xử lý (S-US1.13).
4. Given giáo viên hoặc học viên, When cố xuất bản tóm tắt, Then backend từ chối.

**Truy vết**: FR-031, US1. *(đã chốt V-15)* — định dạng và danh sách trường cụ thể chưa nêu trong spec; quan hệ với "xuất dữ liệu" GĐ2 — xem #V-28.

---

### US2 — Đặt lịch và tổ chức buổi học trực tuyến 1 kèm 1 (P1)

#### S-US2.1 — Khai báo lịch rảnh định kỳ hằng tuần
**As a** giáo viên **I want** khai báo khung giờ rảnh lặp lại hằng tuần **so that** học viên có khung giờ để đặt.

**Acceptance (Given/When/Then)**:
1. Given giáo viên đã đăng nhập, When khai báo rảnh Thứ 2 và Thứ 4, 19:00–21:00, Then các khung giờ này hiển thị là trống cho học viên được phân công trong các tuần tới.
2. Given đã có buổi được đặt trong một khung, When giáo viên sửa/xóa lịch rảnh định kỳ bao trùm khung đó, Then buổi đã đặt không bị ảnh hưởng.
3. Given hai khung rảnh chồng nhau, When lưu, Then hệ thống gộp hoặc báo lỗi (không tạo khung trùng). *(giả định — chưa có trong spec)*

**Truy vết**: FR-008, US2 (Edge Case "Giáo viên thay đổi lịch rảnh")

#### S-US2.2 — Khai báo ngày nghỉ ngoại lệ
**As a** giáo viên **I want** khai báo ngày nghỉ ngoại lệ **so that** học viên không đặt được vào ngày tôi bận.

**Acceptance (Given/When/Then)**:
1. Given giáo viên rảnh định kỳ Thứ 2, When khai báo nghỉ một Thứ 2 cụ thể, Then khung giờ ngày đó không còn hiển thị là trống.
2. Given đã có buổi Đã đặt vào ngày được khai báo nghỉ, When lưu ngày nghỉ, Then hệ thống không lưu và liệt kê các buổi trùng. *(đã chốt V-9)*
3. Given giáo viên hủy các buổi trùng đó, When hủy, Then các buổi chuyển "Giáo viên vắng/hủy", học viên không bị trừ buổi, học viên và giáo vụ được thông báo; sau đó lưu được ngày nghỉ. *(đã chốt V-9)*

**Truy vết**: FR-008, FR-014, FR-013, US2 (Edge Case "Giáo viên thay đổi lịch rảnh ... ngày nghỉ ngoại lệ trùng buổi Đã đặt thì bị chặn")

#### S-US2.3 — Học viên đặt buổi học trong khung trống
**As a** học viên **I want** chọn một khung giờ trống của giáo viên phụ trách để đặt buổi **so that** tôi chủ động lịch học (dưới 1 phút — SC-001).

**Acceptance (Given/When/Then)**:
1. Given giáo viên đã khai báo lịch rảnh và học viên còn buổi trong gói, When học viên chọn một khung giờ trống, Then buổi học được tạo với trạng thái "Đã đặt", khung giờ đó không còn khả dụng với người khác, và cả hai bên nhận xác nhận.
2. Given học viên đặt, When xem khung giờ, Then chỉ thấy khung giờ của giáo viên phụ trách của mình, thời lượng theo gói học.
3. Given học viên không còn buổi hoặc gói hết hạn, When đặt, Then bị chặn (xem S-US5.4).

**Truy vết**: FR-009, FR-010, FR-024, FR-013, US2 (Acceptance 1)

#### S-US2.4 — Chống trùng lịch và đặt đồng thời
**As a** học viên / giáo viên **I want** hệ thống không bao giờ tạo hai buổi trùng giờ cho cùng một người **so that** không xảy ra trùng lịch (SC-003: 0 trường hợp).

**Acceptance (Given/When/Then)**:
1. Given hai học viên cùng đặt một khung giờ của một giáo viên cùng lúc, When cả hai gửi yêu cầu, Then chỉ một lượt thành công, người còn lại được báo khung giờ đã hết.
2. Given học viên đã có một buổi lúc 19:00–19:45, When đặt (hoặc giáo vụ đặt thay) một buổi khác chồng thời gian, Then bị từ chối.
3. Given giáo viên đã có buổi trong khung, When giáo vụ đặt thay một buổi chồng thời gian cho giáo viên đó, Then bị từ chối.

**Truy vết**: FR-010, US2 (Edge Case "Hai học viên cùng đặt")

#### S-US2.5 — Giáo vụ đặt / đổi / hủy thay học viên
**As a** giáo vụ **I want** đặt, đổi, hủy buổi học thay cho bất kỳ học viên nào **so that** tôi hỗ trợ học viên không tự thao tác được và xử lý xếp bù.

**Acceptance (Given/When/Then)**:
1. Given học viên còn buổi, When giáo vụ đặt thay vào khung trống của giáo viên phụ trách, Then buổi được tạo như khi học viên tự đặt và cả hai bên nhận xác nhận.
2. Given buổi đã đặt, When giáo vụ đổi giờ hoặc hủy, Then áp dụng chính sách hủy/đổi (S-US2.6) và hai bên được thông báo.
3. Given buổi chuyển "Giáo viên vắng/hủy", When giáo vụ được thông báo, Then giáo vụ xếp buổi bù với giáo viên phụ trách hoặc đổi giáo viên phụ trách (S-US1.8); học viên không bị trừ buổi. *(đã chốt V-7)*
4. *[GĐ2]* Xếp giáo viên dạy thay cho một buổi — không làm trong MVP. *(đã chốt V-7)*

**Truy vết**: FR-009, FR-011, FR-013, FR-014, FR-006, US2 (Edge Case "Giáo viên hủy buổi hoặc vắng mặt"); Assumptions "Dạy thay thuộc [GĐ2]"

#### S-US2.6 — Học viên hủy / đổi lịch theo hạn chót
**As a** học viên **I want** hủy hoặc đổi buổi học **so that** tôi không mất buổi khi bận đột xuất nếu báo sớm.

**Acceptance (Given/When/Then)**:
1. Given buổi đã đặt và còn hơn hạn chót hủy (mặc định 12 giờ), When học viên hủy, Then buổi chuyển "Hủy đúng hạn" và không bị trừ buổi.
2. Given còn hơn hạn chót, When học viên đổi sang khung trống khác, Then buổi được dời, khung cũ trở lại khả dụng và không bị trừ buổi.
3. Given còn ít hơn hạn chót, When học viên hủy, Then buổi bị tính là đã sử dụng (trừ buổi) và hệ thống cảnh báo trước khi xác nhận.
4. Given còn ít hơn hạn chót, When học viên muốn đổi lịch, Then hệ thống không cho đổi, chỉ cho hủy muộn sau khi cảnh báo buổi sẽ bị trừ; muốn học bù thì đặt buổi mới. *(đã chốt V-5)*

**Truy vết**: FR-011, FR-024, FR-013, US2 (Acceptance 4). Trạng thái lưu cho "hủy muộn" — xem #V-23.

#### S-US2.7 — Hạn chót hủy theo cấu hình triển khai (mốc nhắc lịch cố định)
**As a** giáo vụ **I want** hạn chót hủy được đặt theo chính sách trung tâm (qua cấu hình triển khai `app.scheduling.cancel-cutoff-hours`) **so that** chính sách hủy phù hợp vận hành của trung tâm.

**Acceptance (Given/When/Then)**:
1. Given cấu hình triển khai đặt hạn chót hủy 24 giờ, When học viên hủy buổi còn 20 giờ, Then buổi bị tính là đã sử dụng.
2. Given MVP, When tìm màn hình cấu hình, Then không có; hạn chót hủy chỉ đổi qua cấu hình triển khai, mốc nhắc lịch cố định 24 giờ và 1 giờ. *(đã chốt V-6, cổng duyệt units)*

**Truy vết**: FR-011, FR-013, US2; Assumptions "Mốc nhắc lịch cố định 24h và 1h trong MVP"

#### S-US2.8 — Thông báo khi đặt / đổi / hủy lịch
**As a** giáo viên / học viên **I want** nhận thông báo mỗi khi buổi học của tôi được đặt, đổi hoặc hủy **so that** tôi luôn nắm lịch mới nhất.

**Acceptance (Given/When/Then)**:
1. Given một buổi được đặt/đổi/hủy (bởi bất kỳ ai, kể cả hệ thống tự hủy khi đổi giáo viên hoặc ngừng kích hoạt), When thao tác thành công, Then giáo viên và học viên của buổi nhận email và thông báo trong hệ thống.
2. Given dịch vụ email tạm lỗi, When gửi thông báo, Then thông báo trong hệ thống vẫn được tạo và email được thử gửi lại.

**Truy vết**: FR-013, FR-029, FR-006, US2 (Acceptance 1)

#### S-US2.9 — Nhắc lịch 24 giờ và 1 giờ trước buổi
**As a** giáo viên / học viên **I want** được nhắc trước buổi học **so that** tôi không quên buổi (mục tiêu giảm vắng không báo — SC-004).

**Acceptance (Given/When/Then)**:
1. Given buổi học đã đặt, When còn 24 giờ và 1 giờ trước giờ học, Then giáo viên và học viên nhận nhắc lịch qua email và thông báo trong hệ thống.
2. Given buổi bị hủy trước mốc nhắc, When đến mốc, Then không gửi nhắc.
3. Given buổi được đặt khi còn ít hơn 24 giờ, When đặt xong, Then chỉ gửi các mốc nhắc còn lại trong tương lai.
4. Given nhắc lịch, When hiển thị giờ học, Then theo múi giờ của người nhận.

**Truy vết**: FR-013, US2 (Acceptance 2)

#### S-US2.10 — Vào phòng học trực tuyến tích hợp
**As a** giáo viên / học viên **I want** vào phòng học từ buổi học tương ứng ngay trên website **so that** không cần chuyển sang ứng dụng khác.

**Acceptance (Given/When/Then)**:
1. Given còn hơn 10 phút trước giờ học, When mở buổi học, Then lối vào phòng chưa mở và hiển thị thời điểm mở.
2. Given từ 10 phút trước giờ học, When giáo viên/học viên của buổi mở buổi học, Then vào được phòng học có video, âm thanh, chia sẻ màn hình và chat văn bản.
3. Given một người không phải giáo viên/học viên của buổi (kể cả giáo viên khác), When cố vào phòng, Then bị từ chối.
4. Given buổi đã hủy, When mở buổi, Then không có lối vào phòng.
5. Given đã quá 15 phút sau giờ kết thúc dự kiến, When cố vào phòng, Then lối vào đã đóng. *(đã chốt V-10)*
6. Given nhà cung cấp phòng học được đổi qua cấu hình, When vào phòng, Then nghiệp vụ không thay đổi.

**Truy vết**: FR-012, FR-029, US2 (Acceptance 3)

#### S-US2.11 — Ghi nhận trạng thái buổi học
**As a** giáo viên / giáo vụ **I want** buổi học có trạng thái rõ ràng (Đã đặt, Hoàn thành, Học viên vắng, Giáo viên vắng/hủy, Hủy đúng hạn, Gián đoạn) **so that** số buổi và báo cáo được tính đúng.

**Acceptance (Given/When/Then)**:
1. Given buổi đã diễn ra bình thường, When giáo viên của buổi đánh dấu "Hoàn thành", Then buổi chuyển sang Hoàn thành và bị trừ 1 buổi. *(đã chốt V-4)*
1a. Given đã quá giờ học 24 giờ mà giáo viên chưa đánh dấu kết quả, When đến mốc, Then giáo viên và giáo vụ được nhắc, buổi vẫn Đã đặt (chưa trừ), và giáo vụ được chốt kết quả thay. *(đã chốt V-4)*
2. Given học viên vắng không báo, When ghi nhận "Học viên vắng", Then buổi bị tính là đã sử dụng và lưu vào lịch sử.
3. Given giáo viên vắng hoặc hủy, When ghi nhận "Giáo viên vắng/hủy", Then học viên không bị trừ buổi và giáo vụ được thông báo để xếp bù hoặc đổi giáo viên phụ trách (không có dạy thay trong MVP). *(đã chốt V-7)*
4. Given sự cố kết nối giữa buổi, When giáo viên đánh dấu "Gián đoạn", Then buổi mặc định không bị trừ và giáo vụ được thông báo để quyết định tính là buổi đã học hoặc xếp bù (S-US5.3). *(đã chốt V-4)*

**Truy vết**: FR-014, FR-011, FR-024, US2 (Independent Test; Edge Cases no-show, giáo viên vắng, gián đoạn, giáo viên quên đánh dấu). Danh sách trạng thái chưa đủ — xem #V-23.

#### S-US2.12 — Hiển thị lịch theo múi giờ người xem
**As a** người dùng ở múi giờ khác **I want** mọi lịch và giờ học hiển thị theo múi giờ của tôi **so that** không nhầm giờ học.

**Acceptance (Given/When/Then)**:
1. Given giáo viên ở UTC+7 và học viên ở UTC+9, When cả hai xem cùng một buổi, Then mỗi người thấy giờ theo múi giờ của mình và cùng chỉ một thời điểm thực.
2. Given học viên đổi múi giờ trong hồ sơ, When xem lịch, Then lịch hiển thị theo múi giờ mới.

**Truy vết**: FR-008, FR-009, FR-013, US2 (Edge Case "múi giờ khác nhau")

---

### US3 — Hồ sơ năng lực và lộ trình học cá nhân hóa (P2)

#### S-US3.1 — Ghi kết quả kiểm tra đầu vào theo kỹ năng
**As a** giáo viên phụ trách **I want** ghi kết quả kiểm tra đầu vào chính thức cho từng kỹ năng (Nghe, Nói, Đọc, Viết, Ngữ pháp/Từ vựng) theo CEFR **so that** tôi có cơ sở xây lộ trình.

**Acceptance (Given/When/Then)**:
1. Given học viên mới được phân công, When giáo viên ghi kết quả kiểm tra đầu vào theo từng kỹ năng, Then hồ sơ năng lực hiển thị trình độ hiện tại từng kỹ năng (A1–C2).
2. Given hồ sơ có trình độ sơ bộ (tự khai) do giáo vụ nhập, When giáo viên ghi kết quả đầu vào chính thức, Then kết quả chính thức trở thành nguồn chính của hồ sơ năng lực; trình độ sơ bộ vẫn còn trong lịch sử. *(đã chốt V-11)*
3. Given giá trị ngoài thang CEFR, When lưu, Then bị từ chối.
4. Given học viên không được phân công, When giáo viên cố ghi kết quả, Then backend từ chối.

**Truy vết**: FR-015, FR-016, FR-003, US3 (Acceptance 1), US1 (Acceptance 1)

#### S-US3.2 — Ghi và sửa mục tiêu học tập
**As a** giáo vụ (khi tạo hồ sơ) hoặc giáo viên phụ trách **I want** ghi và sửa mục tiêu học tập (loại mục tiêu, mức cần đạt, thời hạn) **so that** lộ trình bám sát điều học viên cần.

**Acceptance (Given/When/Then)**:
1. Given học viên có hồ sơ, When giáo vụ hoặc giáo viên phụ trách ghi mục tiêu "IELTS 6.5 trong 6 tháng", Then mục tiêu lưu với loại, mức cần đạt và thời hạn, và học viên xem được.
2. Given nhập mục tiêu nhưng thiếu loại hoặc mức cần đạt, When lưu, Then báo lỗi trường bắt buộc (thời hạn tùy chọn).
2a. Given học viên chưa có mục tiêu, When giáo vụ/giáo viên xem danh sách hoặc hồ sơ, Then học viên hiện nhãn "Cần tư vấn" và lọc được theo nhãn này; sau khi ghi mục tiêu thì nhãn mất. *(cổng duyệt units)*
3. Given mục tiêu đã có, When giáo vụ hoặc giáo viên phụ trách sửa, Then mục tiêu mới được lưu và mục tiêu cũ vẫn còn trong lịch sử (ai sửa, khi nào). *(đã chốt V-11)*
4. Given học viên hoặc giáo viên không phụ trách, When cố sửa mục tiêu, Then backend từ chối. *(đã chốt V-11)*

**Truy vết**: FR-015, FR-016, FR-003, US3, US1 (Acceptance 1)

#### S-US3.3 — Đánh giá định kỳ và lịch sử hồ sơ năng lực
**As a** giáo viên **I want** ghi các kỳ đánh giá định kỳ và xem lịch sử trình độ **so that** thấy học viên tiến bộ qua thời gian.

**Acceptance (Given/When/Then)**:
1. Given học viên đã có kết quả đầu vào, When giáo viên ghi một kỳ đánh giá định kỳ, Then trình độ hiện tại cập nhật và kết quả cũ vẫn lưu trong lịch sử kèm ngày ghi nhận.
2. Given hồ sơ có nhiều kỳ đánh giá, When xem lịch sử, Then thấy các mốc theo thứ tự thời gian, từng kỹ năng.

**Truy vết**: FR-016, FR-028, US3

#### S-US3.4 — Giáo viên tự soạn lộ trình cá nhân [Demo: Rút gọn]
**As a** giáo viên **I want** tạo lộ trình gồm các chặng, mỗi chặng có chủ đề/mục tiêu bài học theo thứ tự và mốc thời gian dự kiến **so that** học viên có lộ trình riêng.

**Acceptance (Given/When/Then)**:
1. Given học viên đã có hồ sơ năng lực và mục tiêu, When giáo viên tự soạn lộ trình 3 chặng và gán chủ đề cho từng chặng, Then lộ trình được lưu với thứ tự chặng/chủ đề và mốc dự kiến.
2. Given chặng không có mốc thời gian dự kiến, When lưu, Then báo lỗi. *(theo FR-017: mỗi chặng có mốc thời gian dự kiến)*
3. Given học viên không được phân công, When giáo viên cố tạo lộ trình, Then backend từ chối.
4. *[GĐ2]* Khởi tạo từ mẫu lộ trình — xem S-US3.7. *(đã chốt V-2)*

**Truy vết**: FR-017, FR-003, US3 (Acceptance 2, phần MVP "tự soạn")

#### S-US3.5 — Học viên xem mục tiêu, trình độ và lộ trình
**As a** học viên **I want** xem mục tiêu, trình độ hiện tại và lộ trình của mình **so that** tôi hiểu mình đang ở đâu (SC-009).

**Acceptance (Given/When/Then)**:
1. Given giáo viên đã tạo lộ trình, When học viên đăng nhập và mở lộ trình, Then thấy các chặng, chủ đề, mốc thời gian dự kiến và trạng thái hoàn thành.
2. Given học viên mở hồ sơ năng lực, When xem, Then thấy trình độ hiện tại từng kỹ năng và mục tiêu.
3. Given học viên, When xem lộ trình, Then không có thao tác sửa.

**Truy vết**: FR-015, FR-017, FR-021, US3 (Independent Test, Acceptance 2)

#### S-US3.6 — Điều chỉnh lộ trình kèm lý do và lưu lịch sử
**As a** giáo viên **I want** thêm/bớt/sắp xếp chủ đề và đổi mốc thời gian kèm lý do **so that** lộ trình theo kịp tốc độ tiếp thu của học viên.

**Acceptance (Given/When/Then)**:
1. Given học viên tiến bộ nhanh/chậm hơn dự kiến, When giáo viên điều chỉnh lộ trình và nhập lý do, Then thay đổi được lưu vào lịch sử (ai sửa, khi nào, lý do).
2. Given không nhập lý do, When lưu điều chỉnh, Then bị từ chối.
3. Given lộ trình có lịch sử điều chỉnh, When giáo viên/giáo vụ xem lịch sử, Then thấy danh sách thay đổi theo thời gian.

**Truy vết**: FR-018, FR-028, US3 (Acceptance 3)

#### S-US3.7 — Khởi tạo lộ trình từ mẫu do giáo vụ quản lý [GĐ2]
**As a** giáo viên **I want** khởi tạo lộ trình từ mẫu lộ trình của khóa học **so that** tiết kiệm thời gian soạn.

**Acceptance (Given/When/Then)**:
1. Given giáo vụ đã tạo mẫu lộ trình cho khóa IELTS, When giáo viên tạo lộ trình từ mẫu, Then lộ trình được sao chép từ mẫu và sửa được độc lập với mẫu.

**Truy vết**: FR-017 (phần [GĐ2]), US3 (Acceptance 2, phần [GĐ2] "tạo từ mẫu lộ trình có sẵn"). **Không làm trong MVP** — spec gắn [GĐ2] trực tiếp trong FR/acceptance. *(đã chốt V-2)*

---

### US4 — Đánh giá sau buổi học và theo dõi tiến bộ (P2)

#### S-US4.1 — Giáo viên nộp phiếu đánh giá buổi học
**As a** giáo viên **I want** điền phiếu đánh giá ngay sau buổi (chủ đề lộ trình đã dạy, điểm kỹ năng 1–5, nhận xét, bài tập về nhà) **so that** tiến bộ được lưu có cấu trúc (hoàn thành < 3 phút — SC-008).

**Acceptance (Given/When/Then)**:
1. Given buổi học vừa kết thúc, When giáo viên mở buổi học, Then hệ thống yêu cầu điền phiếu đánh giá (nội dung, điểm kỹ năng, nhận xét, bài tập).
2. Given phiếu, When chọn chủ đề đã dạy, Then chỉ chọn được chủ đề thuộc lộ trình của học viên đó.
3. Given điểm kỹ năng ngoài thang 1–5, When nộp, Then bị từ chối.
4. Given giao bài tập trong phiếu, When giáo viên đặt (hoặc bỏ trống) hạn nộp, Then bài tập được lưu với hạn nộp tùy chọn. *(đã chốt V-17)*
5. Given phiếu đã nộp, When học viên mở buổi học, Then thấy nhận xét và bài tập (S-US4.3).

**Truy vết**: FR-019, FR-020, FR-028, US4 (Acceptance 1)

#### S-US4.2 — Nhắc phiếu quá hạn 24 giờ và báo cho giáo vụ
**As a** giáo vụ **I want** hệ thống nhắc giáo viên chưa nộp phiếu sau 24 giờ và cho tôi thấy trong báo cáo **so that** đạt ≥ 95% phiếu nộp đúng hạn (SC-005).

**Acceptance (Given/When/Then)**:
1. Given giáo viên chưa nộp phiếu sau 24 giờ kể từ khi buổi kết thúc, When hết hạn, Then giáo viên nhận nhắc qua email và thông báo trong hệ thống.
2. Given phiếu quá hạn, When giáo vụ mở báo cáo, Then thấy buổi đó trong danh sách phiếu chưa nộp/nộp muộn.
3. Given phiếu đã nộp trước 24 giờ, When đến mốc, Then không gửi nhắc.

**Truy vết**: FR-019, FR-023, FR-013, US4 (Acceptance 2)

#### S-US4.3 — Học viên xem nhận xét và bài tập từng buổi
**As a** học viên **I want** xem nhận xét, điểm và bài tập của từng buổi **so that** tôi biết cần cải thiện gì.

**Acceptance (Given/When/Then)**:
1. Given giáo viên đã nộp phiếu cho 3 buổi, When học viên mở lịch sử buổi học, Then thấy nhận xét và bài tập của từng buổi.
2. Given phiếu chưa nộp, When học viên mở buổi đó, Then hiển thị "chưa có đánh giá".

**Truy vết**: FR-019, FR-021, US4 (Independent Test)

#### S-US4.4 — Học viên nộp bài tập (văn bản; tệp/âm thanh [GĐ2] theo K2) [Demo: Rút gọn]
**As a** học viên **I want** nộp bài tập dạng văn bản hoặc tệp đính kèm (kể cả tệp âm thanh) **so that** giáo viên chấm và phản hồi.

**Acceptance (Given/When/Then)**:
1. Given giáo viên đã giao bài tập, When học viên nộp văn bản hoặc tải lên tệp, Then bài nộp được lưu và giáo viên nhận thông báo.
2. Given tệp > 20 MB hoặc định dạng ngoài pdf, doc/docx, jpg, png, mp3, m4a, wav, webm, When tải lên, Then bị từ chối với thông báo rõ giới hạn. *(đã chốt V-18)*
3. Given bài tập có hạn nộp đã qua, When học viên nộp, Then bài vẫn được nhận và được đánh dấu "nộp trễ". *(đã chốt V-17)*
4. Given học viên khác, When cố xem bài nộp, Then backend từ chối.
5. *[GĐ2]* Ghi âm trực tiếp trên web — xem S-US4.7.

**Truy vết**: FR-020, FR-029 (lưu trữ tệp), US4 (Acceptance 3, phần MVP)

#### S-US4.5 — Giáo viên chấm và phản hồi bài tập
**As a** giáo viên **I want** chấm điểm và phản hồi bài nộp **so that** kết quả được cộng vào lịch sử học tập.

**Acceptance (Given/When/Then)**:
1. Given học viên đã nộp bài, When giáo viên chấm điểm (thang 0–10) và viết phản hồi, Then học viên nhận thông báo và thấy điểm, phản hồi; kết quả cộng vào lịch sử học tập.
2. Given điểm ngoài thang 0–10, When lưu, Then bị từ chối. *(đã chốt V-17)*
3. Given bài nộp của học viên không được phân công, When giáo viên cố chấm, Then backend từ chối.

**Truy vết**: FR-020, FR-028, FR-003, US4 (Acceptance 3)

#### S-US4.6 — Trang tiến bộ [Demo: Rút gọn]
**As a** học viên / giáo viên phụ trách / giáo vụ **I want** xem trang tiến bộ (lịch sử buổi, nhận xét, biểu đồ điểm kỹ năng theo thời gian, % hoàn thành lộ trình) **so that** thấy rõ học viên tiến bộ thế nào.

**Acceptance (Given/When/Then)**:
1. Given học viên có nhiều buổi đã đánh giá, When mở trang tiến bộ, Then thấy lịch sử buổi, nhận xét, diễn biến điểm từng kỹ năng theo thời gian và % hoàn thành lộ trình.
2. Given hoàn thành 3 buổi có đánh giá, When học viên xem biểu đồ, Then biểu đồ có 3 điểm dữ liệu mỗi kỹ năng được chấm.
3. Given giáo viên không phụ trách, When cố mở trang tiến bộ của học viên, Then backend từ chối.
4. *[GĐ2]* Tỷ lệ chuyên cần và tỷ lệ nộp bài tập — không có trong demo. *(đã chốt V-3)*

**Truy vết**: FR-021, FR-003, US4 (Acceptance 4, Independent Test)

#### S-US4.7 — Ghi âm bài nộp trực tiếp trên web [GĐ2]
**As a** học viên **I want** ghi âm trực tiếp trên trình duyệt để nộp bài nói **so that** không phải dùng ứng dụng ghi âm riêng.

**Acceptance (Given/When/Then)**:
1. Given bài tập nói, When học viên ghi âm trên web và nộp, Then bản ghi được lưu như một bài nộp.

**Truy vết**: FR-020 (phần [GĐ2]), US4 (Acceptance 3, phần [GĐ2] "ghi âm trực tiếp trên web"). **Không làm trong MVP**. *(đã chốt V-2)*

---

### US5 — Quản lý số buổi học và học phí (P3)

#### S-US5.1 — Ghi nhận thanh toán và kích hoạt gói
**As a** giáo vụ **I want** ghi nhận thủ công thanh toán học phí (số tiền, ngày, phương thức chuyển khoản/tiền mặt, ghi chú) **so that** gói được kích hoạt và có lịch sử thanh toán.

**Acceptance (Given/When/Then)**:
1. Given ghi danh "Chờ thanh toán", When giáo vụ ghi nhận khoản thanh toán đầu tiên (kể cả chưa đủ học phí), Then gói được kích hoạt, hạn sử dụng tính từ ngày kích hoạt, phần còn thiếu thành công nợ, và lịch sử thanh toán lưu số tiền, ngày, phương thức, ghi chú, người ghi nhận.
1a. Given gói đã kích hoạt, When giáo vụ ghi nhận thêm thanh toán, Then công nợ giảm nhưng ngày kích hoạt/hết hạn không đổi.
2. Given số tiền ≤ 0, When lưu, Then bị từ chối.
3. Given giáo viên hoặc học viên, When cố ghi nhận thanh toán, Then backend từ chối.

**Truy vết**: FR-025, US5 (Acceptance 1). Thanh toán trực tuyến: GĐ2. Quy tắc kích hoạt đã chốt V-1 (clarify 2026-10-18).

#### S-US5.2 — Xem công nợ và lịch sử thanh toán
**As a** giáo vụ (và học viên với dữ liệu của mình) **I want** xem công nợ và lịch sử thanh toán **so that** biết ai còn nợ học phí (chính xác 100% — SC-007).

**Acceptance (Given/When/Then)**:
1. Given ghi danh có học phí (sao từ giá gói lúc ghi danh) và các khoản đã thanh toán, When giáo vụ xem công nợ học viên, Then công nợ = học phí của ghi danh − tổng đã thanh toán, khớp với lịch sử thanh toán. *(đã chốt V-13)*
2. Given học viên thanh toán nhiều lần, When xem, Then công nợ giảm tương ứng từng lần.
3. Given giáo vụ giảm giá cho một ghi danh, When nhập học phí khác giá gói lúc tạo ghi danh, Then công nợ tính theo học phí đó; các ghi danh khác của cùng gói không đổi; sau khi tạo không sửa được học phí. *(đã chốt V-13, cổng duyệt units)*
4. Given học viên đăng nhập, When mở trang gói/thanh toán, Then thấy (chỉ đọc) lịch sử thanh toán và công nợ của chính mình. *(đã chốt V-13)*

**Truy vết**: FR-025, FR-001, US5

#### S-US5.3 — Tự động cập nhật số buổi đã học / đã đặt / còn lại
**As a** giáo vụ / học viên **I want** số buổi đã học/đã đặt/còn lại tự cập nhật theo trạng thái buổi học **so that** không phải đếm thủ công (chính xác 100% — SC-007).

**Acceptance (Given/When/Then)**:
1. Given gói 12 buổi, When 10 buổi "Hoàn thành", Then số buổi còn lại là 2.
2. Given buổi "Hủy đúng hạn", "Giáo viên vắng/hủy" hoặc bị hủy tự động do đổi giáo viên / ngừng kích hoạt học viên, When cập nhật, Then không trừ buổi và trả lại chỗ. *(đã chốt V-8, V-16)*
3. Given buổi "Học viên vắng" hoặc hủy muộn, When cập nhật, Then trừ buổi.
4. Given buổi "Gián đoạn", When chưa có quyết định, Then không bị trừ; When giáo vụ chọn "tính là đã học", Then trừ 1 buổi; When chọn "xếp bù", Then trả lại chỗ và không trừ. *(đã chốt V-4)*
5. Given học viên mở trang gói, When xem, Then thấy số buổi đã học, đã đặt, còn lại và hạn sử dụng. *(đã chốt V-4)*

**Truy vết**: FR-024, FR-011, FR-014, FR-004, FR-006, US5, US2 (Independent Test)

#### S-US5.4 — Chặn đặt lịch khi gói hết buổi hoặc hết hạn
**As a** học viên **I want** được báo rõ khi không đặt được vì hết buổi/hết hạn **so that** tôi biết cần gia hạn.

**Acceptance (Given/When/Then)**:
1. Given học viên hết buổi, When cố đặt lịch mới, Then hệ thống chặn và hướng dẫn gia hạn.
2. Given gói đã hết hạn dù còn buổi, When đặt lịch, Then hệ thống chặn (các buổi còn lại bị khóa); các buổi Đã đặt trước đó vẫn giữ.
3. Given học viên còn 1 buổi và đã có 1 buổi "Đã đặt", When đặt thêm, Then bị chặn (buổi Đã đặt giữ chỗ; còn lại = tổng − đã học − đã đặt). *(đã chốt V-4)*

**Truy vết**: FR-024, US5 (Acceptance 3; Edge Case "Gói học hết hạn khi vẫn còn buổi")

#### S-US5.5 — Nhắc gia hạn gói
**As a** học viên / giáo vụ **I want** được nhắc khi gói còn ≤ 2 buổi hoặc còn ≤ 7 ngày đến hạn **so that** gia hạn kịp thời.

**Acceptance (Given/When/Then)**:
1. Given gói còn ≤ 2 buổi hoặc còn ≤ 7 ngày đến hạn, When đến ngưỡng, Then học viên và giáo vụ nhận nhắc gia hạn qua email và thông báo trong hệ thống.
2. Given đã nhắc theo một lý do (≤ 2 buổi hoặc ≤ 7 ngày) cho một ghi danh, When lý do đó vẫn đúng ở các ngày sau, Then không gửi lặp; lý do còn lại vẫn được nhắc một lần khi tới ngưỡng. *(đã chốt V-19)*

**Truy vết**: FR-026, FR-013, US5 (Acceptance 2, Independent Test)

#### S-US5.6 — Gia hạn thủ công ghi danh đã hết hạn
**As a** giáo vụ **I want** gia hạn thủ công một ghi danh đã hết hạn **so that** học viên học nốt các buổi còn lại.

**Acceptance (Given/When/Then)**:
1. Given ghi danh Hết hạn còn 3 buổi bị khóa, When giáo vụ đặt ngày hết hạn mới, Then ghi danh dùng lại được và học viên đặt lịch được 3 buổi đó.
2. Given thao tác gia hạn, When lưu, Then hệ thống ghi lại người thực hiện và thời điểm. *(đã chốt V-20)*
3. Given ghi danh có buổi Đã đặt trước khi hết hạn, When ghi danh hết hạn rồi được gia hạn, Then các buổi Đã đặt đó vẫn giữ nguyên. *(đã chốt V-20)*

**Truy vết**: FR-024, US5 (Edge Case "Gói học hết hạn khi vẫn còn buổi")

#### S-US5.7 — Báo cáo tổng hợp tài chính và số buổi theo giáo viên [Demo: Rút gọn]
**As a** giáo vụ **I want** xem tổng đã thu, công nợ, số buổi đã dạy theo giáo viên trong khoảng thời gian chọn **so that** theo dõi doanh thu và tính thù lao giáo viên.

**Acceptance (Given/When/Then)**:
1. Given có thanh toán và buổi học trong tháng, When chọn khoảng thời gian tháng đó, Then thấy tổng đã thu, tổng công nợ và số buổi đã dạy của từng giáo viên.
2. Given số liệu báo cáo, When đối chiếu với lịch sử thanh toán và buổi học (kể cả của hồ sơ đã ẩn danh hóa), Then khớp 100%.

**Truy vết**: FR-027, FR-025, FR-031, US5. "Buổi đã dạy" = mọi buổi học viên bị trừ (Hoàn thành, Học viên vắng, Hủy muộn, Gián đoạn được tính) — đã chốt V-4.

---

### US6 — Theo dõi chất lượng giảng dạy (P3)

#### S-US6.1 — Học viên đánh giá buổi học (1–5 sao)
**As a** học viên **I want** đánh giá buổi học đã hoàn thành bằng 1–5 sao và nhận xét tùy chọn **so that** trung tâm biết chất lượng giảng dạy.

**Acceptance (Given/When/Then)**:
1. Given buổi học hoàn thành, When học viên mở hệ thống, Then được mời đánh giá buổi học (không bắt buộc).
2. Given học viên chọn 4 sao không kèm nhận xét, When gửi trong vòng 7 ngày sau buổi, Then đánh giá được lưu.
3. Given buổi chưa "Hoàn thành" (đã hủy, vắng…), When học viên cố đánh giá, Then không cho phép.
4. Given buổi đã được đánh giá, When học viên cố sửa hoặc đánh giá lại, Then bị từ chối. *(đã chốt V-12)*
5. Given đã quá 7 ngày sau buổi, When học viên cố đánh giá, Then không cho phép. *(đã chốt V-12)*
6. Given đánh giá đã gửi, When giáo viên của buổi mở hệ thống, Then không thấy đánh giá (chỉ giáo vụ xem trong MVP). *(đã chốt V-12)*

**Truy vết**: FR-022, FR-001, US6 (Acceptance 1)

#### S-US6.2 — Báo cáo chất lượng theo giáo viên [GĐ2 — cắt theo K1, 2026-10-18]
**As a** giáo vụ **I want** xem báo cáo theo giáo viên và khoảng thời gian: điểm hài lòng trung bình, số buổi dạy, tỷ lệ hủy/vắng (phía giáo viên và phía học viên), tỷ lệ nộp phiếu đúng hạn **so that** quản lý chất lượng giảng dạy.

**Acceptance (Given/When/Then)**:
1. Given có dữ liệu từ nhiều buổi, When giáo vụ mở báo cáo chất lượng theo khoảng thời gian, Then thấy các chỉ số trên cho từng giáo viên.
2. Given giáo viên có 10 buổi đã đặt trong kỳ (không có Hủy đúng hạn hay hủy do đổi giáo viên), trong đó 1 "Giáo viên vắng/hủy", 1 "Học viên vắng" và 1 hủy muộn, When xem, Then tỷ lệ hủy/vắng phía giáo viên là 10% và phía học viên là 20%. *(đã chốt V-21; mẫu số khi có Hủy đúng hạn — xem #V-27)*
3. Given buổi bị hủy tự động do đổi giáo viên phụ trách, When tính tỷ lệ, Then không tính vào tỷ lệ hủy của giáo viên. *(đã chốt V-8, V-21)*
4. Given giáo viên có 10 buổi Hoàn thành, 9 phiếu nộp trong 24 giờ sau buổi, When xem, Then tỷ lệ nộp phiếu đúng hạn là 90%. *(đã chốt V-21)*
5. Given số buổi dạy, When đối chiếu với S-US5.7, Then khớp định nghĩa "buổi đã dạy" của FR-027. *(đã chốt V-21)*
6. Given giáo viên chưa có đánh giá sao nào, When xem, Then điểm hài lòng hiển thị "chưa có dữ liệu" thay vì 0.

**Truy vết**: FR-023, FR-022, FR-019, FR-027, US6 (Acceptance 2, Independent Test)

#### S-US6.3 — Xem chi tiết từng buổi từ báo cáo chất lượng [GĐ2 — cắt theo K1, 2026-10-18]
**As a** giáo vụ **I want** từ báo cáo chất lượng mở chi tiết từng buổi **so that** xác minh nguyên nhân chỉ số thấp.

**Acceptance (Given/When/Then)**:
1. Given báo cáo chất lượng của một giáo viên, When chọn giáo viên, Then thấy danh sách buổi trong kỳ với trạng thái, đánh giá sao, nhận xét và thời điểm nộp phiếu.
2. Given giáo viên, When cố mở báo cáo hoặc chi tiết này, Then backend từ chối (giáo viên không xem đánh giá từng buổi trong MVP). *(đã chốt V-12)*

**Truy vết**: FR-023, FR-022, US6 (Acceptance 2 "xem chi tiết từng buổi")

#### S-US6.4 — Chỉ số tỷ lệ đúng giờ và tiến bộ trung bình [GĐ2]
**As a** giáo vụ **I want** báo cáo có tỷ lệ buổi dạy đúng giờ và mức tiến bộ trung bình của học viên **so that** đánh giá giáo viên đầy đủ.

**Acceptance (Given/When/Then)**:
1. Given đã có định nghĩa "đúng giờ" và "mức tiến bộ trung bình" (cần chốt khi vào GĐ2), When giáo vụ mở báo cáo, Then thấy hai chỉ số này theo từng giáo viên.

**Truy vết**: FR-023 (phần [GĐ2]), US6 (mô tả, phần [GĐ2]). **Không làm trong MVP**. *(đã chốt V-2, V-21 — công thức hai chỉ số này để GĐ2)*

---

## Truy vết FR → Stories

| FR | Stories | Ghi chú |
|----|---------|---------|
| FR-001 | S-US1.1, S-US1.10, S-US5.2, S-US6.1 | |
| FR-002 | S-US1.1, S-US1.2, S-US1.3, S-US1.11 | |
| FR-003 | S-US1.8, S-US1.9, S-US1.10, S-US3.1, S-US3.2, S-US3.4, S-US4.5, S-US4.6 | |
| FR-004 | S-US1.3, S-US1.4, S-US1.5, S-US1.12, S-US1.14, S-US5.3 | Kích hoạt lại / ngừng kích hoạt đã chốt (V-16); ngoại lệ ẩn danh hóa (FR-031) |
| FR-005 | S-US1.6 | |
| FR-006 | S-US1.5, S-US1.7, S-US1.8, S-US2.5, S-US2.8, S-US5.3 | Tự hủy buổi tương lai khi đổi giáo viên (V-8) |
| FR-007 | S-US1.9 | Demo rút gọn: tên + trạng thái/giáo viên |
| FR-008 | S-US2.1, S-US2.2, S-US2.12 | Chặn ngày nghỉ trùng buổi Đã đặt (V-9) |
| FR-009 | S-US2.3, S-US2.5, S-US2.12 | |
| FR-010 | S-US2.3, S-US2.4 | |
| FR-011 | S-US2.5, S-US2.6, S-US2.7, S-US2.11, S-US5.3 | |
| FR-012 | S-US2.10 | Mở −10 phút, đóng +15 phút (V-10) |
| FR-013 | S-US2.2, S-US2.3, S-US2.5, S-US2.6, S-US2.7, S-US2.8, S-US2.9, S-US2.12, S-US4.2, S-US5.5 | Mốc nhắc cố định 24h/1h (V-6) |
| FR-014 | S-US2.2, S-US2.5, S-US2.11, S-US5.3 | Danh sách trạng thái chưa đủ — #V-23 |
| FR-015 | S-US1.4, S-US3.1, S-US3.2, S-US3.5 | |
| FR-016 | S-US1.4, S-US3.1, S-US3.2, S-US3.3 | Nguồn dữ liệu và quyền sửa mục tiêu (V-11) |
| FR-017 | S-US3.4, S-US3.5, S-US3.7 [GĐ2] | Phần "mẫu lộ trình" là [GĐ2] |
| FR-018 | S-US3.6 | |
| FR-019 | S-US4.1, S-US4.2, S-US4.3, S-US6.2 | |
| FR-020 | S-US4.1, S-US4.4, S-US4.5, S-US4.7 [GĐ2] | Hạn nộp, thang 0–10, giới hạn tệp (V-17, V-18); "ghi âm" là [GĐ2] |
| FR-021 | S-US3.5, S-US4.3, S-US4.6 | Chuyên cần / nộp bài tập là [GĐ2] (V-3) |
| FR-022 | S-US1.10, S-US6.1, S-US6.2, S-US6.3 | 7 ngày, không sửa, chỉ giáo vụ xem (V-12) |
| FR-023 | S-US1.8, S-US4.2, S-US6.2, S-US6.3, S-US6.4 [GĐ2] | Công thức đã chốt (V-21); đúng giờ, tiến bộ TB là [GĐ2] |
| FR-024 | S-US1.7, S-US1.8, S-US2.3, S-US2.6, S-US2.11, S-US5.3, S-US5.4, S-US5.6 | Gia hạn thủ công (V-20) |
| FR-025 | S-US1.6, S-US1.7, S-US1.10, S-US5.1, S-US5.2, S-US5.7 | Công thức công nợ, học viên xem chỉ đọc (V-13) |
| FR-026 | S-US5.5 | Kênh và tần suất (V-19) |
| FR-027 | S-US5.7, S-US6.2 | Demo rút gọn |
| FR-028 | (một phần) S-US3.3, S-US3.6, S-US4.1, S-US4.5 | Chỉ phần "lưu có cấu trúc"; không có story riêng — xem Gaps |
| FR-029 | (một phần) S-US2.8, S-US2.10, S-US4.4 | Yêu cầu kỹ thuật, không có story riêng — xem Gaps |
| FR-030 | S-US1.3, S-US1.11, S-US1.12 | Mới (clarify 2026-10-18; V-14) |
| FR-031 | S-US1.4, S-US1.13, S-US1.14, S-US1.15, S-US5.7 | Mới (clarify 2026-10-18; V-15) |

## Gaps

FR / tiêu chí không có story người dùng riêng (chỉ được chạm gián tiếp):

- **FR-028** (dữ liệu có cấu trúc, xuất được, phục vụ AI): phần "lưu có cấu trúc" được đảm bảo qua acceptance của S-US3.3, S-US3.6, S-US4.1, S-US4.5; phần "xuất được" bị spec xếp vào "Để sau (GĐ2+)". Đề xuất: xử lý như yêu cầu dữ liệu/NFR ở stage thiết kế; không tạo story trong MVP. Lưu ý khác biệt với "xuất bản tóm tắt" của FR-031 (S-US1.15, MVP) — xem #V-28.
- **FR-029** (thay nhà cung cấp chỉ qua cấu hình): là ràng buộc kiến trúc (constitution II — Ports & Adapters, adapter giả lập), không phải hành vi người dùng. Đề xuất: theo dõi ở stage NFR/kiến trúc (port `VideoRoomProvider`, email, lưu trữ tệp, thanh toán, AI) thay vì story.
- **SC-010** (200 buổi đồng thời): đã chốt V-22 — đo sau ra mắt; demo chỉ kiểm ở mức backend (lượt vào phòng và thao tác của 200 buổi, không đo chất lượng video). Là NFR, không có story; theo dõi trong nfr.md / kiểm thử tải.

Giả định còn lại (chưa có trong spec):
- S-US2.1 Acceptance 3: hai khung rảnh chồng nhau thì gộp hoặc báo lỗi.
- S-US1.11 Acceptance 5: chặn nghiệp vụ khi người dùng chưa đồng ý lại phiên bản chính sách mới — xem #V-25.

Phụ thuộc pháp lý bên ngoài (spec đã ghi, cần trung tâm xác nhận trước khi ra mắt): mốc 16 tuổi và nội dung điều khoản/chính sách dữ liệu (Assumptions; FR-030).

Các hành vi trước đây có trong spec nhưng thiếu FR (V-7, V-8, V-14, V-15, V-20) nay đã có FR hoặc được gắn [GĐ2]; không còn mục loại này.

## Vấn đề spec / đề xuất /speckit-clarify

| # | Loại | Vị trí trong spec.md | Mô tả | Đề xuất |
|---|------|----------------------|-------|---------|
| V-1 | ✅ Đã chốt 2026-10-18: kích hoạt khi có thanh toán đầu tiên — Mâu thuẫn | US1 Acceptance 2 ↔ US5 Acceptance 1 | US1: ghi danh xong học viên "có 24 buổi khả dụng". US5: gói chỉ "được kích hoạt" khi giáo vụ ghi nhận thanh toán. Không rõ ghi danh chưa thanh toán có đặt lịch được không; có cho học trước trả sau (công nợ) không. (data-model.md đã tự chọn `PENDING_PAYMENT → ACTIVE` nhưng spec chưa chốt.) | Clarify: gói kích hoạt khi ghi danh hay khi có thanh toán đầu tiên / đủ tiền? Sửa US1 Acceptance 2 cho khớp. |
| V-2 | ✅ Đã chốt 2026-10-18: tách phần MVP và phần [GĐ2] ngay trong FR/acceptance; demo chỉ kiểm phần MVP — Mâu thuẫn phạm vi | US3 Acceptance 2, US4 Acceptance 3, US6, FR-017, FR-020, FR-023 ↔ bảng "Phạm vi bản demo MVP" | Acceptance scenario và FR yêu cầu đầy đủ, nhưng bảng phạm vi xếp các phần này vào Rút gọn/Để sau. | Đã áp dụng: S-US3.7, S-US4.7, S-US6.4 gắn [GĐ2]. |
| V-3 | ✅ Đã chốt 2026-10-18: tỷ lệ chuyên cần và nộp bài tập là [GĐ2] — Mơ hồ phạm vi | FR-021 ↔ bảng phạm vi | Không rõ hai tỷ lệ này có trong demo không. | Đã áp dụng: S-US4.6 Acceptance 4 gắn [GĐ2]. |
| V-4 | ✅ Đã chốt 2026-10-18 (4 câu, xem spec Clarifications) — Thiếu định nghĩa | FR-014, FR-024, Edge Case "Gián đoạn", FR-027 | Không nêu ai/cơ chế nào chuyển buổi sang "Hoàn thành" (tự động khi hết giờ, giáo viên xác nhận, hay khi nộp phiếu); buổi "Gián đoạn" trừ buổi hay không; buổi "Đã đặt" có giữ chỗ trong số buổi còn lại không; "số buổi đã dạy" trong FR-027 có tính "Học viên vắng" không. | Clarify bảng trạng thái → trừ buổi → tính thù lao. |
| V-5 | ✅ Đã chốt 2026-10-18: chặn đổi lịch sau hạn chót — Mơ hồ | US2 Acceptance 4, FR-011 | US2 nói "hủy/đổi" sau hạn chót bị tính đã sử dụng; FR-011 chỉ nói "hủy muộn hoặc vắng". Không rõ đổi lịch muộn bị chặn, bị trừ buổi, hay được phép. | Clarify quy tắc đổi lịch sau hạn chót. |
| V-6 | ✅ Đã chốt 2026-10-18: mốc nhắc cố định 24h/1h trong MVP; chỉ hạn chót hủy cấu hình được — Mâu thuẫn nhẹ | FR-013 ↔ Assumptions | FR-013 ghi cố định; Assumptions cũ cho cấu hình. | Đã áp dụng: S-US2.7. |
| V-7 | ✅ Đã chốt 2026-10-18: dạy thay là [GĐ2]; giáo viên vắng → Giáo viên vắng/hủy (không trừ), giáo vụ xếp bù hoặc đổi giáo viên phụ trách — Thiếu FR / mâu thuẫn quyền | Assumptions ↔ FR-003, FR-012 | Không có FR về dạy thay và quyền của giáo viên dạy thay. | Đã áp dụng: S-US2.5, S-US2.11, persona P2. |
| V-8 | ✅ Đã chốt 2026-10-18: đổi giáo viên → buổi Đã đặt tương lai tự hủy, không trừ, không tính tỷ lệ hủy, thông báo 3 bên (FR-006) — Thiếu | FR-006, Edge Case "đổi giáo viên phụ trách" | Buổi đã đặt tương lai với giáo viên cũ xử lý thế nào. | Đã áp dụng: S-US1.8, S-US5.3, S-US6.2. |
| V-9 | ✅ Đã chốt 2026-10-18: chặn lưu ngày nghỉ trùng buổi Đã đặt; giáo viên hủy trước (Giáo viên vắng/hủy, không trừ, báo giáo vụ) (FR-008) — Thiếu | FR-008, Edge Case "Giáo viên thay đổi lịch rảnh" | Ngày nghỉ ngoại lệ trùng buổi đã đặt. | Đã áp dụng: S-US2.2. |
| V-10 | ✅ Đã chốt 2026-10-18: phòng mở −10 phút, đóng +15 phút sau giờ kết thúc dự kiến (FR-012) — Thiếu | FR-012 | Không có thời điểm đóng phòng. | Đã áp dụng: S-US2.10. |
| V-11 | ✅ Đã chốt 2026-10-18: giáo vụ nhập trình độ sơ bộ + mục tiêu; giáo viên phụ trách ghi đầu vào chính thức (nguồn chính); mục tiêu do giáo vụ/giáo viên phụ trách sửa, có lịch sử (FR-016) — Mơ hồ | US1 Acceptance 1 ↔ FR-015, FR-016 | Ai là nguồn dữ liệu chính và quyền sửa mục tiêu. | Đã áp dụng: S-US1.4, S-US3.1, S-US3.2. |
| V-12 | ✅ Đã chốt 2026-10-18: chỉ giáo vụ xem đánh giá từng buổi; giáo viên không xem; đánh giá trong 7 ngày, không sửa (FR-022) — Thiếu | FR-022, FR-023 | Quyền xem, sửa, hạn chót đánh giá. | Đã áp dụng: S-US1.10, S-US6.1, S-US6.3, persona P2. |
| V-13 | ✅ Đã chốt 2026-10-18: công nợ = học phí ghi danh (sao từ giá gói, sửa được khi giảm giá) − đã thanh toán; sửa giá gói không ảnh hưởng ghi danh cũ; học viên xem chỉ đọc (FR-025) — Thiếu định nghĩa | FR-025, FR-027, Key Entity Payment | Công thức công nợ và quyền xem của học viên. | Đã áp dụng: S-US1.6, S-US5.2, S-US1.10, persona P3. |
| V-14 | ✅ Đã chốt 2026-10-18 qua FR-030: mọi người dùng đồng ý khi đặt mật khẩu lần đầu (lưu phiên bản + thời điểm, đồng ý lại khi đổi phiên bản); dưới 16 tuổi cần giáo vụ ghi nhận đồng ý người giám hộ trước khi kích hoạt — Thiếu (pháp lý) | Tổng quan, Assumptions, khóa "Tiếng Anh thiếu nhi" | Thiếu yêu cầu đồng ý xử lý dữ liệu trẻ em. | Đã áp dụng: S-US1.3, S-US1.11, S-US1.12. Mốc tuổi chờ pháp lý xác nhận. |
| V-15 | ✅ Đã chốt 2026-10-18 qua FR-031: nhật ký yêu cầu chủ thể dữ liệu; yêu cầu xóa → ẩn danh hóa; yêu cầu xem → xuất bản tóm tắt — Thiếu FR | Constitution V ↔ FR-004 | Thiếu quy trình xử lý yêu cầu xóa/truy cập. | Đã áp dụng: S-US1.13, S-US1.14, S-US1.15. |
| V-16 | ✅ Đã chốt 2026-10-18: kích hoạt lại được; ngừng học viên → hủy buổi tương lai không trừ; ngừng giáo viên bị chặn tới khi đổi hết học viên (FR-004) — Thiếu | FR-004 | Kích hoạt lại và ngừng kích hoạt khi còn buổi/học viên. | Đã áp dụng: S-US1.4, S-US1.5, S-US5.3. |
| V-17 | ✅ Đã chốt 2026-10-18: hạn nộp tùy chọn, nộp trễ vẫn nhận nhưng đánh dấu; thang 0–10 (FR-020) — Thiếu | FR-019, FR-020, Key Entity Assignment | Hạn nộp và thang điểm bài tập. | Đã áp dụng: S-US4.1, S-US4.4, S-US4.5. |
| V-18 | ✅ Đã chốt 2026-10-18: ≤ 20 MB/tệp; pdf, doc/docx, jpg, png, mp3, m4a, wav, webm (FR-020) — Thiếu | FR-020 | Giới hạn dung lượng/định dạng tệp. | Đã áp dụng: S-US4.4. |
| V-19 | ✅ Đã chốt 2026-10-18: email + thông báo trong hệ thống; mỗi lý do nhắc một lần cho mỗi ghi danh (FR-026) — Thiếu | FR-026 | Kênh, tần suất nhắc gia hạn. | Đã áp dụng: S-US5.5. |
| V-20 | ✅ Đã chốt 2026-10-18: giáo vụ đặt ngày hết hạn mới; ghi người + thời điểm; buổi Đã đặt giữ nguyên (FR-024) — Thiếu FR | Edge Case "Gói học hết hạn khi vẫn còn buổi" | Gia hạn thủ công không có FR. | Đã áp dụng: S-US5.6. |
| V-21 | ✅ Đã chốt 2026-10-18: hài lòng = TB sao; số buổi dạy theo FR-027; hủy/vắng tách phía giáo viên / học viên; nộp phiếu đúng hạn = phiếu ≤ 24h / buổi Hoàn thành; đúng giờ, tiến bộ TB là [GĐ2] (FR-023) — Thiếu định nghĩa | FR-023, US6 | Công thức các chỉ số. | Đã áp dụng: S-US6.2, S-US6.4. Còn điểm mơ hồ về mẫu số — #V-27. |
| V-22 | ✅ Đã chốt 2026-10-18: SC-010 đo sau ra mắt; demo chỉ kiểm backend 200 buổi đồng thời, không đo chất lượng video — Rủi ro NFR | SC-010 ↔ gói miễn phí | SC-010 không rõ đo ở demo hay sau ra mắt. | Đã áp dụng: Gaps (NFR). |
| V-23 | ✅ Đã chốt 2026-10-18: thêm trạng thái Hủy muộn, Hủy hệ thống; trạng thái ghi danh đầy đủ — Mâu thuẫn / thiếu định nghĩa | FR-014 (danh sách trạng thái) ↔ FR-011, FR-023, FR-027 ("Hủy muộn"), FR-004, FR-006 (hủy tự động), Key Entity Enrollment, Clarifications ("ghi danh Hết hạn") | FR-014 liệt kê 6 trạng thái buổi nhưng không có "Hủy muộn", trong khi FR-023 và FR-027 dùng "Hủy muộn" như một loại buổi riêng. Buổi bị hủy tự động do đổi giáo viên / ngừng kích hoạt học viên (không trừ, không tính tỷ lệ hủy) cũng không có trạng thái tương ứng (không phải "Hủy đúng hạn" — vì khác nghĩa — cũng không phải "Giáo viên vắng/hủy" — vì sẽ bị tính vào tỷ lệ hủy giáo viên). Kết quả quyết định của buổi Gián đoạn (tính đã học / xếp bù) chưa có trạng thái. Key Entity Enrollment chỉ có "Chờ thanh toán → Đang dùng", thiếu "Hết hạn" (dùng trong Clarifications) và trạng thái sau ẩn danh hóa. | Clarify: bổ sung FR-014 các trạng thái "Hủy muộn", "Hủy do hệ thống" (hoặc lý do hủy), kết quả Gián đoạn; bổ sung trạng thái ghi danh "Hết hạn". |
| V-24 | ✅ Đã chốt 2026-10-18: ngày sinh bắt buộc, tuổi tính lúc kích hoạt, học viên tự tích + giám hộ — Thiếu | FR-030, Key Entity Student ("gồm ngày sinh") | Spec không nói ngày sinh là bắt buộc; nếu thiếu ngày sinh thì không xác định được học viên dưới 16 tuổi. Chưa nói mốc tuổi tính tại thời điểm nào (tạo hồ sơ / kích hoạt) và có cần làm gì khi học viên đủ 16 tuổi. Cũng chưa rõ với tài khoản dùng chung phụ huynh–học viên dưới 16 tuổi thì ai tích đồng ý ở bước đặt mật khẩu lần đầu. | Clarify: ngày sinh bắt buộc với học viên? mốc tính tuổi? |
| V-25 | ✅ Đã chốt 2026-10-18: chặn mọi chức năng tới khi đồng ý lại; phiên bản qua cấu hình — Thiếu | FR-030 ("yêu cầu đồng ý lại khi chính sách đổi phiên bản") | Không nói người dùng từ chối/chưa đồng ý phiên bản mới thì bị chặn những gì (đăng nhập, đặt lịch, vào phòng học…), và ai/cách nào phát hành phiên bản chính sách mới (giáo vụ trên giao diện hay cấu hình triển khai). | Clarify hành vi khi chưa đồng ý lại và cơ chế phát hành phiên bản. |
| V-26 | ✅ Đã chốt 2026-10-18: xóa toàn bộ dữ liệu học tập; giữ buổi/sao/tài chính không định danh; giữ bản ghi yêu cầu — Mơ hồ | FR-031 (phạm vi ẩn danh hóa) ↔ Key Entity Data subject request, FR-015..FR-019 | FR-031 liệt kê dữ liệu xóa và dữ liệu giữ, nhưng không nói về hồ sơ năng lực, mục tiêu, lộ trình, điểm kỹ năng từng buổi, chủ đề đã dạy, điểm bài tập và nhận xét kèm đánh giá sao (xóa hay giữ không định danh). Ngoài ra bản ghi yêu cầu xóa lưu "người yêu cầu" (định danh) — chưa rõ có được giữ sau ẩn danh hóa (làm bằng chứng tuân thủ) hay cũng phải xóa. | Clarify danh sách đầy đủ dữ liệu xóa / giữ sau ẩn danh hóa. |
| V-27 | ✅ Đã chốt 2026-10-18: loại Hủy đúng hạn + Hủy hệ thống khỏi mẫu số — Mơ hồ | FR-023 ("trên tổng buổi đã đặt trong kỳ (không tính Hủy đúng hạn và hủy do đổi giáo viên)") | Không rõ "không tính" nghĩa là loại các buổi này khỏi mẫu số, hay chỉ không tính vào tử số. Hai cách cho kết quả khác nhau khi có buổi Hủy đúng hạn. Cũng chưa rõ buổi hủy do ngừng kích hoạt học viên / trước ẩn danh hóa có bị loại tương tự không. | Clarify mẫu số tỷ lệ hủy/vắng. |
| V-28 | ✅ Đã chốt 2026-10-18: trang tóm tắt in được thuộc MVP; GĐ2 chỉ là xuất cho AI — Mâu thuẫn nhẹ phạm vi | FR-031 ("xuất được bản tóm tắt", demo Đầy đủ) ↔ bảng phạm vi "Để sau: xuất dữ liệu (FR-028 chỉ cần lưu có cấu trúc)" | Bảng phạm vi đẩy "xuất dữ liệu" sang GĐ2+, trong khi FR-031 yêu cầu xuất bản tóm tắt dữ liệu cá nhân trong MVP; định dạng (PDF/CSV/JSON) và danh sách trường của bản tóm tắt chưa nêu. | Ghi rõ trong bảng phạm vi rằng "xuất dữ liệu GĐ2" chỉ là xuất cho AI (FR-028), không gồm bản tóm tắt FR-031; chốt định dạng tối thiểu. |
