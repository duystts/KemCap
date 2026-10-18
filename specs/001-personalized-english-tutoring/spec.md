# Feature Specification: Nền tảng đào tạo tiếng Anh 1 kèm 1 cá nhân hóa (Personalized English Learning Platform)

**Feature Branch**: `001-personalized-english-tutoring`

**Created**: 2026-10-12

**Status**: Draft

**Input**: User description: "Website quản lý và đào tạo tiếng Anh 1 kèm 1 — Personalized English Learning Platform (nền tảng quản lý đào tạo tiếng Anh cá nhân hóa), không chỉ là website quản lý học viên, giáo viên và lịch học. Điểm khác biệt: mỗi học viên có hồ sơ năng lực, mục tiêu học tập, lộ trình riêng và được theo dõi tiến bộ sau từng buổi học. Hệ thống cần giải quyết 6 nhu cầu: (1) Quản lý tập trung thông tin học viên, giáo viên, khóa học và lịch học. (2) Tổ chức lớp học tiếng Anh trực tuyến 1 giáo viên – 1 học viên. (3) Xây dựng lộ trình học tập cá nhân hóa theo trình độ, mục tiêu và tốc độ tiếp thu. (4) Theo dõi chất lượng giảng dạy, kết quả học tập và sự tiến bộ của từng học viên. (5) Tự động hóa đặt lịch, nhắc lịch, quản lý số buổi học và học phí. (6) Ứng dụng AI hỗ trợ đánh giá năng lực, đề xuất bài tập và cá nhân hóa lộ trình — thuộc các giai đoạn phát triển tiếp theo (ngoài phạm vi MVP)."

## Tổng quan & Phạm vi

**Vai trò người dùng**:

- **Quản trị viên / Giáo vụ (Admin)**: quản lý toàn bộ dữ liệu, xếp giáo viên, quản lý gói học, học phí và báo cáo.
- **Giáo viên (Teacher)**: khai báo lịch rảnh, dạy buổi học, ghi nhận đánh giá sau buổi, cập nhật lộ trình cho học viên được phân công.
- **Học viên (Student)**: xem/đặt lịch, vào lớp, xem lộ trình, tiến bộ, bài tập và số buổi còn lại.
- **Phụ huynh**: không có tài khoản riêng. Nếu học viên nhỏ tuổi, phụ huynh dùng chung tài khoản học viên.

**Trong phạm vi MVP**: nhu cầu (1) đến (5).

### Phạm vi bản demo MVP (1 tháng, hạn 2026-11-12)

| Mức độ | Nội dung |
|---|---|
| **Đầy đủ** | FR-001–FR-014 (tài khoản, quản lý dữ liệu, lịch rảnh, đặt/đổi/hủy lịch, chống trùng, phòng học tích hợp, nhắc lịch email, trạng thái buổi) · FR-015–FR-016 (hồ sơ năng lực, kiểm tra đầu vào) · FR-019 (phiếu đánh giá + nhắc sau 24h) · FR-022 (học viên đánh giá sao) · FR-024–FR-026 (trừ buổi, học phí thủ công, công nợ, nhắc gia hạn) · FR-029 · FR-030 (đồng ý xử lý dữ liệu) · FR-031 (yêu cầu xóa/xem dữ liệu, ẩn danh hóa) |
| **Rút gọn** | FR-007: tìm theo tên + lọc theo trạng thái/giáo viên · FR-017–FR-018: lộ trình do giáo viên tự soạn (chưa có mẫu lộ trình), sửa có ghi lý do · FR-020: nộp bài dạng văn bản (tệp, tệp âm thanh, ghi âm trực tiếp: GĐ2) · FR-021: lịch sử buổi + biểu đồ điểm kỹ năng + % hoàn thành lộ trình · FR-023: không có trong demo (GĐ2); demo chỉ thu đánh giá sao (FR-022) · FR-027: tổng đã thu, công nợ, số buổi theo giáo viên |
| **Để sau (GĐ2+)** | Mẫu lộ trình · ghi âm trực tiếp · thanh toán trực tuyến · ghi hình buổi học · nhắc qua SMS/Zalo · xuất dữ liệu học tập cho AI (FR-028 chỉ cần lưu có cấu trúc; không gồm trang tóm tắt dữ liệu cá nhân của FR-031) · ứng dụng di động · AI |

SC-002, SC-004 và SC-009 cần dữ liệu vận hành thực tế (vài tháng), nên được đo sau khi ra mắt, không đo trong bản demo.

**Ngoài phạm vi MVP**: nhu cầu (6) – các tính năng AI (đánh giá năng lực tự động, đề xuất bài tập, tự điều chỉnh lộ trình). MVP chỉ cần lưu dữ liệu có cấu trúc (điểm kỹ năng, nhận xét, kết quả bài tập) để giai đoạn sau có thể khai thác.

## Clarifications

### Session 2026-10-12

- Q: Phòng học trực tuyến tích hợp hay dùng link ngoài? → A: Tích hợp video ngay trên website.
- Q: Thanh toán học phí? → A: MVP giáo vụ ghi nhận thủ công; thanh toán trực tuyến ở giai đoạn 2.
- Q: Tài khoản phụ huynh? → A: Không cần.
- Q: Ngân sách dịch vụ trả phí? → A: Chưa có; demo dùng gói miễn phí, thiết kế để dễ thay nhà cung cấp.

### Session 2026-10-18

- Q: Gói học của học viên bắt đầu dùng được (đặt lịch được) từ lúc nào? → A: Ghi danh xong gói ở trạng thái "Chờ thanh toán" (chưa đặt lịch được); gói kích hoạt khi giáo vụ ghi nhận khoản thanh toán đầu tiên (số tiền bất kỳ), phần còn thiếu là công nợ; hạn sử dụng tính từ ngày kích hoạt.
- Q: Ai đánh dấu một buổi học đã "Hoàn thành", và nếu không ai đánh dấu thì sao? → A: Giáo viên đánh dấu kết quả buổi (Hoàn thành / Học viên vắng / Gián đoạn); sau 24 giờ chưa đánh dấu thì giáo viên và giáo vụ được nhắc, giáo vụ chốt thay; buổi chưa chốt chưa bị trừ.
- Q: Một buổi học bị gián đoạn giữa chừng có bị trừ khỏi gói của học viên không? → A: Mặc định không trừ; giáo vụ xem xét và chọn tính là buổi đã học (trừ buổi) hoặc xếp buổi bù (không trừ).
- Q: Khi tính thù lao giáo viên, loại buổi nào được tính là "buổi đã dạy"? → A: Mọi buổi mà học viên bị trừ: Hoàn thành, Học viên vắng, Hủy muộn, và Gián đoạn được giáo vụ tính là đã học.
- Q: Học viên muốn đổi lịch khi đã quá hạn chót thì hệ thống xử lý thế nào? → A: Chặn đổi lịch sau hạn chót; học viên chỉ có thể giữ buổi hoặc hủy muộn (được cảnh báo trước là bị trừ buổi); muốn học bù thì đặt buổi mới (trừ thêm 1 buổi).
- Q: Khi học viên đặt một buổi nhưng chưa học, số "buổi còn lại" có bị giảm ngay không? → A: Có — buổi Đã đặt giữ chỗ ngay: còn lại = tổng − đã học − đã đặt; không đặt vượt số còn lại; hiển thị đã học / đã đặt / còn lại; hủy đúng hạn trả lại chỗ.
- Q: Hệ thống phải ghi nhận việc đồng ý cho xử lý dữ liệu cá nhân như thế nào, nhất là với học viên là trẻ em? → A: Mọi người dùng tích đồng ý điều khoản & chính sách dữ liệu khi đặt mật khẩu lần đầu (lưu phiên bản + thời điểm); học viên dưới 16 tuổi (theo ngày sinh) cần giáo vụ ghi nhận đồng ý của cha mẹ/người giám hộ trước khi kích hoạt tài khoản. (Mốc tuổi và nội dung điều khoản cần pháp lý trung tâm xác nhận.)
- Q: Khi học viên (hoặc phụ huynh) yêu cầu xóa hoặc xem dữ liệu cá nhân của mình thì MVP xử lý thế nào? → A: Giáo vụ nhận yêu cầu qua kênh ngoài và ghi nhận trong hệ thống; yêu cầu xóa → ẩn danh hóa hồ sơ (xóa thông tin định danh, tệp bài nộp, nhận xét; giữ số buổi, thanh toán, đánh giá ở dạng ẩn danh); yêu cầu xem → giáo vụ xuất bản tóm tắt dữ liệu của học viên.
- Q: Các phần đã xếp "Rút gọn/Để sau" nhưng còn trong acceptance và FR được ghi thế nào? → A: Tách ngay trong FR/acceptance: phần MVP ghi trước, phần còn lại gắn nhãn [GĐ2]; acceptance của demo chỉ kiểm phần MVP.
- Q: Trang tiến bộ trong demo có tỷ lệ chuyên cần và tỷ lệ nộp bài tập không? → A: Không; [GĐ2]. Demo gồm lịch sử buổi, nhận xét, biểu đồ điểm kỹ năng, % hoàn thành lộ trình. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Mốc nhắc lịch 24h/1h có cấu hình được không? → A: Cố định 24h và 1h trong MVP; chỉ hạn chót hủy là cấu hình được. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: MVP có hỗ trợ giáo viên dạy thay không? → A: Không; [GĐ2]. Giáo viên vắng → buổi Giáo viên vắng/hủy (không trừ), giáo vụ xếp bù hoặc đổi giáo viên phụ trách. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Khi đổi giáo viên phụ trách, các buổi Đã đặt trong tương lai với giáo viên cũ xử lý thế nào? → A: Tự động hủy, không trừ buổi, không tính vào tỷ lệ hủy của giáo viên; học viên và hai giáo viên được thông báo; học viên đặt lại với giáo viên mới. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Giáo viên khai báo ngày nghỉ trùng buổi Đã đặt thì sao? → A: Hệ thống chặn lưu và liệt kê buổi trùng; giáo viên phải hủy các buổi đó trước (thành Giáo viên vắng/hủy, không trừ, giáo vụ được thông báo). *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Phòng học đóng lúc nào? → A: Mở trước giờ bắt đầu 10 phút, đóng 15 phút sau giờ kết thúc dự kiến. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Ai ghi trình độ đầu vào và mục tiêu? → A: Giáo vụ nhập trình độ sơ bộ (tự khai) và mục tiêu khi tạo hồ sơ; giáo viên phụ trách ghi kết quả kiểm tra đầu vào chính thức (nguồn chính của hồ sơ năng lực); mục tiêu do giáo vụ hoặc giáo viên phụ trách sửa, có lưu lịch sử. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Ai xem được đánh giá sao của học viên, có sửa được không? → A: Chỉ giáo vụ xem đánh giá từng buổi; giáo viên không xem trong MVP; đánh giá trong vòng 7 ngày sau buổi và không sửa được sau khi gửi. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Công nợ tính thế nào và học viên có xem được không? → A: Công nợ = học phí của ghi danh (sao từ giá gói lúc ghi danh, giáo vụ sửa được khi giảm giá) − tổng đã thanh toán; sửa giá gói không ảnh hưởng ghi danh đã có; học viên xem được (chỉ đọc) lịch sử thanh toán và công nợ của mình. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Kích hoạt lại và ngừng kích hoạt khi còn buổi/học viên thì sao? → A: Kích hoạt lại được; ngừng kích hoạt học viên → các buổi Đã đặt tương lai bị hủy không trừ buổi; ngừng kích hoạt giáo viên bị chặn cho tới khi mọi học viên của giáo viên đó được đổi giáo viên phụ trách. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Bài tập có hạn nộp và thang điểm nào? → A: Hạn nộp tùy chọn do giáo viên đặt; nộp sau hạn vẫn nhận nhưng đánh dấu trễ; thang điểm 0–10. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Giới hạn tệp nộp bài là bao nhiêu? → A: ≤ 20 MB mỗi tệp; định dạng pdf, doc/docx, jpg, png, mp3, m4a, wav, webm. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Nhắc gia hạn gửi qua kênh nào, bao lâu một lần? → A: Email + thông báo trong hệ thống; mỗi lý do (≤ 2 buổi, ≤ 7 ngày) nhắc một lần cho mỗi ghi danh. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Gia hạn thủ công gói hết hạn thế nào? → A: Giáo vụ đặt ngày hết hạn mới cho ghi danh Hết hạn → ghi danh dùng lại được; ghi lại người và thời điểm gia hạn; buổi Đã đặt trước đó vẫn giữ. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Các chỉ số chất lượng trong MVP tính thế nào? → A: Điểm hài lòng = trung bình sao; số buổi dạy = buổi đã dạy (FR-027); tỷ lệ hủy/vắng tách phía giáo viên (Giáo viên vắng/hủy) và phía học viên (Học viên vắng + Hủy muộn) trên tổng buổi đã đặt trong kỳ, không tính Hủy đúng hạn và hủy do đổi giáo viên; tỷ lệ nộp phiếu đúng hạn = phiếu nộp ≤ 24h sau buổi / buổi Hoàn thành. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Mục tiêu học tập có bắt buộc không? → A: Không bắt buộc; học viên chưa có định hướng được đánh dấu "Cần tư vấn" để giáo vụ/giáo viên tư vấn rồi ghi mục tiêu sau. *(người dùng chốt tại cổng duyệt units)*
- Q: Học phí của ghi danh có sửa được sau khi tạo không? → A: Không; giá (kể cả giảm giá) chỉ nhập lúc tạo ghi danh. *(người dùng chốt tại cổng duyệt units)*
- Q: Hạn chót hủy cấu hình ở đâu? → A: Chỉ qua cấu hình triển khai; MVP không có màn hình cấu hình. *(người dùng chốt tại cổng duyệt units)*
- Q: Cắt giảm phạm vi demo để vừa năng lực backend một người? → A: K1 — báo cáo chất lượng giảng dạy (FR-023) chuyển [GĐ2], vẫn thu đánh giá sao (FR-022) và báo cáo số buổi đã dạy (FR-027); K2 — nộp bài tập chỉ dạng văn bản trong demo, nộp tệp/âm thanh chuyển [GĐ2]. *(người dùng duyệt tại cổng units, 2026-10-18)*
- Q: SC-010 (200 buổi đồng thời) đo khi nào? → A: Đo sau ra mắt (khi dùng gói video trả phí); trong demo chỉ kiểm ở mức backend (nhận 200 buổi đồng thời, không đo chất lượng video). *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Danh sách trạng thái buổi học và ghi danh đầy đủ là gì? → A: Buổi: Đã đặt, Hoàn thành, Học viên vắng, Hủy muộn, Hủy đúng hạn, Giáo viên vắng/hủy, Gián đoạn, Hủy hệ thống (do đổi giáo viên / ngừng kích hoạt học viên / ẩn danh hóa — không trừ buổi, không tính tỷ lệ hủy). Gián đoạn được giáo vụ tính là đã học → Hoàn thành; xếp bù → giữ Gián đoạn và đặt buổi mới. Ghi danh: Chờ thanh toán, Đang dùng, Hết buổi, Hết hạn, Đã hủy. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Ngày sinh học viên có bắt buộc không, tuổi tính lúc nào, ai tích đồng ý khi dưới 16 tuổi? → A: Ngày sinh bắt buộc với học viên; tuổi tính tại thời điểm kích hoạt tài khoản; đủ 16 tuổi sau đó không cần làm gì thêm trong MVP; học viên dưới 16 tuổi vẫn tự tích đồng ý khi đặt mật khẩu, bên cạnh đồng ý của người giám hộ do giáo vụ ghi nhận. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Người dùng chưa đồng ý phiên bản chính sách mới thì sao, ai phát hành phiên bản? → A: Sau khi đăng nhập chỉ thấy màn hình đồng ý, không dùng được chức năng khác cho tới khi đồng ý; phiên bản chính sách phát hành qua cấu hình triển khai (không có màn hình quản lý trong MVP). *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Ẩn danh hóa xóa và giữ những dữ liệu nào? → A: Xóa: họ tên, liên hệ, ngày sinh, đồng ý của người giám hộ, hồ sơ năng lực, mục tiêu, lộ trình, điểm kỹ năng và nhận xét trong phiếu đánh giá, bài tập/bài nộp (nội dung, tệp, điểm, phản hồi), nhận xét kèm đánh giá sao. Giữ không định danh: buổi học (thời gian, giáo viên, trạng thái), thời điểm nộp phiếu, số sao, ghi danh, thanh toán, công nợ. Bản ghi yêu cầu xóa (người yêu cầu, ngày, kết quả) được giữ làm bằng chứng tuân thủ. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Mẫu số tỷ lệ hủy/vắng trong báo cáo chất lượng là gì? → A: Mẫu số = số buổi đã đặt trong kỳ, loại bỏ Hủy đúng hạn và Hủy hệ thống. *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*
- Q: Bản tóm tắt dữ liệu cá nhân (yêu cầu xem) có định dạng gì, có mâu thuẫn với "xuất dữ liệu GĐ2" không? → A: Trang tóm tắt in được (lưu PDF qua trình duyệt) liệt kê dữ liệu cá nhân đang lưu; thuộc MVP. "Xuất dữ liệu" ở GĐ2 chỉ là xuất dữ liệu học tập cho AI (FR-028). *(mặc định theo khuyến nghị, người dùng chấp nhận gộp)*

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quản lý tập trung học viên, giáo viên và gói học (Priority: P1)

Giáo vụ tạo và quản lý hồ sơ học viên (thông tin liên hệ, trình độ đầu vào, mục tiêu học tập), hồ sơ giáo viên (chuyên môn, chứng chỉ, khung giờ dạy), và các khóa học/gói học (ví dụ: Giao tiếp, IELTS, Tiếng Anh thiếu nhi; gói 12/24/48 buổi). Giáo vụ phân công một giáo viên phụ trách cho từng học viên.

**Why this priority**: Đây là nền tảng dữ liệu cho mọi chức năng khác; thiếu nó thì không thể xếp lịch, theo dõi hay tính học phí.

**Independent Test**: Giáo vụ tạo được 1 giáo viên, 1 học viên, 1 gói học, ghi danh học viên vào gói và phân công giáo viên; tìm kiếm và xem lại đầy đủ thông tin.

**Acceptance Scenarios**:

1. **Given** giáo vụ đã đăng nhập, **When** tạo hồ sơ học viên với họ tên, liên hệ, trình độ sơ bộ (tự khai) và mục tiêu (tùy chọn — để trống thì hồ sơ hiện "Cần tư vấn"), **Then** hồ sơ được lưu và xuất hiện trong danh sách học viên có thể tìm kiếm/lọc.
2. **Given** đã có học viên, giáo viên và gói học, **When** giáo vụ ghi danh học viên vào gói 24 buổi và phân công giáo viên, **Then** gói ở trạng thái "Chờ thanh toán" (chưa đặt lịch được) và giáo viên thấy học viên trong danh sách của mình; khi giáo vụ ghi nhận khoản thanh toán đầu tiên, học viên có 24 buổi khả dụng.
3. **Given** giáo viên đăng nhập, **When** mở danh sách học viên, **Then** chỉ thấy học viên được phân công cho mình.

---

### User Story 2 - Đặt lịch và tổ chức buổi học trực tuyến 1 kèm 1 (Priority: P1)

Giáo viên khai báo khung giờ rảnh. Học viên (hoặc giáo vụ thay mặt) chọn khung giờ trống của giáo viên phụ trách để đặt buổi học. Đến giờ, cả giáo viên và học viên vào phòng học trực tuyến từ buổi học tương ứng trên hệ thống. Hệ thống tự gửi nhắc lịch trước buổi học.

**Why this priority**: Buổi học 1 kèm 1 là hoạt động cốt lõi tạo ra doanh thu; tự động hóa đặt/nhắc lịch giảm tải lớn cho giáo vụ.

**Independent Test**: Giáo viên khai báo lịch rảnh, học viên đặt một buổi, cả hai nhận nhắc lịch và vào được lớp đúng giờ; buổi học được đánh dấu hoàn thành và trừ 1 buổi trong gói.

**Acceptance Scenarios**:

1. **Given** giáo viên đã khai báo lịch rảnh và học viên còn buổi trong gói, **When** học viên chọn một khung giờ trống, **Then** buổi học được tạo, khung giờ đó không còn khả dụng với người khác, và cả hai bên nhận xác nhận.
2. **Given** buổi học đã đặt, **When** còn 24 giờ và 1 giờ trước giờ học, **Then** giáo viên và học viên nhận nhắc lịch.
3. **Given** đến giờ học, **When** giáo viên/học viên mở buổi học trên hệ thống, **Then** họ vào được phòng học trực tuyến của buổi đó. Phòng học (video, âm thanh, chia sẻ màn hình, chat) được tích hợp ngay trên website, không cần chuyển sang ứng dụng khác.
4. **Given** học viên muốn hủy/đổi lịch, **When** thực hiện trước hạn chót hủy (mặc định 12 giờ trước buổi học), **Then** buổi học được hủy/đổi và không bị trừ buổi; nếu sau hạn chót, hệ thống không cho đổi lịch, chỉ cho hủy muộn sau khi cảnh báo buổi sẽ bị tính là đã sử dụng.

---

### User Story 3 - Hồ sơ năng lực và lộ trình học cá nhân hóa (Priority: P2)

Mỗi học viên có hồ sơ năng lực theo 4 kỹ năng (Nghe, Nói, Đọc, Viết) cùng ngữ pháp/từ vựng, quy về thang trình độ chung (CEFR A1–C2), kèm mục tiêu cụ thể (ví dụ: IELTS 6.5 trong 6 tháng, giao tiếp công việc). Dựa trên bài kiểm tra đầu vào, giáo viên xây dựng lộ trình riêng gồm các chặng (milestone) và chủ đề bài học theo thứ tự, có thể điều chỉnh theo tốc độ tiếp thu.

**Why this priority**: Đây là điểm khác biệt cốt lõi của nền tảng so với website quản lý thông thường.

**Independent Test**: Ghi nhận kết quả đầu vào cho một học viên, tạo lộ trình 3 chặng, học viên đăng nhập xem được mục tiêu, trình độ hiện tại và lộ trình của mình.

**Acceptance Scenarios**:

1. **Given** học viên mới, **When** giáo viên ghi kết quả kiểm tra đầu vào theo từng kỹ năng, **Then** hồ sơ năng lực hiển thị trình độ hiện tại từng kỹ năng.
2. **Given** học viên đã có hồ sơ năng lực và mục tiêu, **When** giáo viên tự soạn lộ trình ([GĐ2]: tạo từ mẫu lộ trình có sẵn) và gán chặng/chủ đề, **Then** học viên thấy lộ trình với các chặng, chủ đề và mốc thời gian dự kiến.
3. **Given** học viên tiến bộ nhanh/chậm hơn dự kiến, **When** giáo viên điều chỉnh lộ trình, **Then** thay đổi được lưu lại lịch sử (ai sửa, khi nào, lý do).

---

### User Story 4 - Đánh giá sau buổi học và theo dõi tiến bộ (Priority: P2)

Sau mỗi buổi học, giáo viên ghi nhận: nội dung đã dạy (gắn với chủ đề trong lộ trình), điểm đánh giá từng kỹ năng trong buổi, nhận xét, và bài tập về nhà. Học viên nộp bài tập, giáo viên chấm. Học viên/giáo vụ xem biểu đồ tiến bộ theo thời gian và mức độ hoàn thành lộ trình.

**Why this priority**: Theo dõi tiến bộ sau từng buổi là cam kết chính với học viên; cũng là dữ liệu cho AI ở giai đoạn sau.

**Independent Test**: Hoàn thành 3 buổi học có đánh giá, học viên xem được nhận xét từng buổi và biểu đồ điểm kỹ năng qua 3 buổi.

**Acceptance Scenarios**:

1. **Given** buổi học vừa kết thúc, **When** giáo viên mở buổi học, **Then** hệ thống yêu cầu điền phiếu đánh giá buổi học (nội dung, điểm kỹ năng, nhận xét, bài tập).
2. **Given** giáo viên chưa nộp đánh giá sau 24 giờ, **When** hết hạn, **Then** hệ thống nhắc giáo viên và hiển thị trong báo cáo của giáo vụ.
3. **Given** giáo viên giao bài tập, **When** học viên nộp bài dạng văn bản ([GĐ2]: tệp, tệp âm thanh, ghi âm trực tiếp trên web), **Then** giáo viên nhận thông báo, chấm và phản hồi; kết quả cộng vào lịch sử học tập.
4. **Given** học viên có nhiều buổi đã đánh giá, **When** mở trang tiến bộ, **Then** thấy diễn biến điểm từng kỹ năng theo thời gian và % hoàn thành lộ trình.

---

### User Story 5 - Quản lý số buổi học và học phí (Priority: P3)

Giáo vụ quản lý gói học đã mua của từng học viên: số buổi đã mua, đã học, còn lại, hạn sử dụng gói; ghi nhận thanh toán học phí và theo dõi công nợ. Hệ thống tự nhắc gia hạn khi gói sắp hết buổi hoặc sắp hết hạn.

**Why this priority**: Cần thiết cho vận hành kinh doanh, nhưng giai đoạn đầu có thể tạm xử lý thủ công nên xếp sau trải nghiệm học tập.

**Independent Test**: Ghi danh học viên vào gói 12 buổi, ghi nhận thanh toán, học 10 buổi và kiểm tra hệ thống gửi nhắc gia hạn, số buổi còn lại chính xác.

**Acceptance Scenarios**:

1. **Given** học viên mua gói, **When** giáo vụ ghi nhận khoản thanh toán đầu tiên (số tiền bất kỳ, kể cả chưa đủ học phí), **Then** gói được kích hoạt, hạn sử dụng tính từ ngày kích hoạt, phần còn thiếu ghi vào công nợ, và lịch sử thanh toán được lưu (số tiền, ngày, phương thức, người ghi nhận). (MVP: giáo vụ ghi nhận thủ công; thanh toán trực tuyến ở giai đoạn 2.)
2. **Given** gói còn 2 buổi hoặc còn 7 ngày đến hạn, **When** đến ngưỡng, **Then** học viên và giáo vụ nhận nhắc gia hạn.
3. **Given** học viên hết buổi, **When** cố đặt lịch mới, **Then** hệ thống chặn và hướng dẫn gia hạn.

---

### User Story 6 - Theo dõi chất lượng giảng dạy (Priority: P3)

Sau mỗi buổi, học viên đánh giá buổi học (1–5 sao và nhận xét). Giáo vụ xem bảng tổng hợp theo giáo viên: điểm hài lòng trung bình, số buổi dạy, tỷ lệ hủy/vắng, tỷ lệ nộp đánh giá đúng hạn; [GĐ2]: tỷ lệ buổi dạy đúng giờ và mức tiến bộ trung bình của học viên.

**Why this priority**: Quan trọng cho quản lý chất lượng nhưng cần có dữ liệu từ các story trước mới có ý nghĩa.

**Independent Test**: Sau một số buổi học có đánh giá từ học viên, giáo vụ mở bảng chất lượng và thấy chỉ số đúng của từng giáo viên.

**Acceptance Scenarios**:

1. **Given** buổi học hoàn thành, **When** học viên mở hệ thống, **Then** được mời đánh giá buổi học (không bắt buộc).
2. [GĐ2] **Given** có dữ liệu từ nhiều buổi, **When** giáo vụ mở báo cáo chất lượng theo khoảng thời gian, **Then** thấy các chỉ số theo từng giáo viên và có thể xem chi tiết từng buổi.

---

### Edge Cases

- Hai học viên cùng đặt một khung giờ của một giáo viên cùng lúc: chỉ một lượt đặt thành công, người còn lại được báo khung giờ đã hết.
- Giáo viên hủy buổi hoặc vắng mặt: buổi không bị trừ của học viên, giáo vụ được thông báo để xếp bù/đổi giáo viên.
- Yêu cầu xóa dữ liệu của học viên còn buổi Đã đặt hoặc còn công nợ: khi ẩn danh hóa, các buổi Đã đặt chuyển Hủy hệ thống (không trừ buổi); công nợ vẫn được giữ ở dạng không định danh.
- Học viên dưới 16 tuổi chưa có đồng ý của cha mẹ/người giám hộ: hồ sơ được tạo nhưng tài khoản không kích hoạt (không gửi lời mời, không đăng nhập) cho tới khi giáo vụ ghi nhận đồng ý.
- Học viên vắng mặt không báo (no-show): buổi bị tính là đã sử dụng; ghi nhận vào lịch sử.
- Giáo viên quên đánh dấu kết quả buổi: buổi giữ trạng thái Đã đặt (chưa trừ); sau 24 giờ hệ thống nhắc giáo viên và giáo vụ, giáo vụ chốt kết quả thay.
- Sự cố kết nối giữa buổi học: giáo viên có thể đánh dấu buổi "Gián đoạn". Buổi Gián đoạn mặc định không bị trừ; giáo vụ xem xét và chọn tính là buổi đã học (trừ buổi) hoặc xếp buổi bù (không trừ).
- Học viên và giáo viên ở các múi giờ khác nhau: lịch luôn hiển thị theo múi giờ của người xem.
- Giáo viên nghỉ việc/đổi giáo viên phụ trách: lịch sử học tập, đánh giá và lộ trình của học viên được giữ nguyên và chuyển cho giáo viên mới.
- Gói học hết hạn khi vẫn còn buổi: không đặt được buổi mới; các buổi Đã đặt trước đó vẫn giữ. Giáo vụ có thể gia hạn thủ công (FR-024).
- Giáo viên thay đổi lịch rảnh định kỳ khi đã có buổi được đặt trong khung đó: các buổi đã đặt không bị ảnh hưởng. Riêng ngày nghỉ ngoại lệ trùng buổi Đã đặt thì bị chặn (xem FR-008).

## Requirements *(mandatory)*

### Functional Requirements

**Tài khoản & phân quyền**

- **FR-001**: Hệ thống MUST hỗ trợ các vai trò Quản trị viên/Giáo vụ, Giáo viên, Học viên với quyền hạn tách biệt; mỗi người chỉ xem/sửa dữ liệu trong phạm vi vai trò.
- **FR-002**: Người dùng MUST đăng nhập bằng email và mật khẩu, có chức năng quên/đặt lại mật khẩu. Tài khoản giáo viên và học viên do giáo vụ tạo hoặc mời.
- **FR-003**: Giáo viên MUST chỉ truy cập được hồ sơ, lộ trình và kết quả của học viên được phân công.

**Quản lý dữ liệu tập trung (nhu cầu 1)**

- **FR-004**: Giáo vụ MUST tạo, sửa, ngừng kích hoạt (không xóa vĩnh viễn) hồ sơ học viên, giáo viên. Hồ sơ đã ngừng kích hoạt có thể được kích hoạt lại. Ngừng kích hoạt học viên thì các buổi Đã đặt trong tương lai chuyển Hủy hệ thống (không trừ buổi); ngừng kích hoạt giáo viên bị chặn cho tới khi mọi học viên của giáo viên đó được đổi giáo viên phụ trách. Ngoại lệ duy nhất cho việc không xóa là ẩn danh hóa theo yêu cầu xóa hợp lệ (FR-031).
- **FR-005**: Giáo vụ MUST định nghĩa khóa học (chương trình) và gói học (số buổi, thời lượng mỗi buổi, thời hạn sử dụng, giá).
- **FR-006**: Giáo vụ MUST ghi danh học viên vào gói học và phân công giáo viên phụ trách; có thể đổi giáo viên mà không mất dữ liệu học tập. Khi đổi giáo viên, các buổi Đã đặt trong tương lai với giáo viên cũ chuyển Hủy hệ thống (không trừ buổi, không tính vào tỷ lệ hủy); học viên và cả hai giáo viên được thông báo.
- **FR-007**: Hệ thống MUST cho phép tìm kiếm và lọc học viên/giáo viên/buổi học theo tên, trạng thái, khóa học, giáo viên và khoảng thời gian.

**Lớp học trực tuyến & đặt lịch (nhu cầu 2, 5)**

- **FR-008**: Giáo viên MUST khai báo lịch rảnh định kỳ hàng tuần và các ngày nghỉ ngoại lệ. Không lưu được ngày nghỉ ngoại lệ trùng buổi Đã đặt: hệ thống liệt kê các buổi trùng để giáo viên hủy trước (buổi chuyển Giáo viên vắng/hủy, không trừ buổi, giáo vụ được thông báo).
- **FR-009**: Học viên MUST đặt buổi học trong các khung giờ trống của giáo viên phụ trách; giáo vụ MUST đặt/đổi/hủy thay cho bất kỳ học viên nào.
- **FR-010**: Hệ thống MUST ngăn trùng lịch cho cả giáo viên và học viên.
- **FR-011**: Hệ thống MUST áp dụng chính sách hủy/đổi lịch: hủy trước hạn chót (mặc định 12 giờ, cấu hình qua cấu hình triển khai) không trừ buổi; hủy muộn hoặc vắng không báo thì trừ buổi. Đổi lịch chỉ được phép trước hạn chót; sau hạn chót hệ thống chặn đổi lịch, chỉ cho hủy muộn (có cảnh báo trước là bị trừ buổi).
- **FR-012**: Mỗi buổi học MUST có lối vào phòng học trực tuyến cho đúng giáo viên và học viên của buổi đó, mở trước giờ học 10 phút và đóng 15 phút sau giờ kết thúc dự kiến. Phòng học được tích hợp trên website, hỗ trợ video, âm thanh, chia sẻ màn hình và chat văn bản trong buổi học.
- **FR-013**: Hệ thống MUST gửi nhắc lịch qua email (và thông báo trong hệ thống) 24 giờ và 1 giờ trước buổi học, và thông báo khi có đặt/đổi/hủy lịch.
- **FR-014**: Hệ thống MUST ghi nhận trạng thái buổi học: Đã đặt, Hoàn thành, Học viên vắng, Hủy muộn, Hủy đúng hạn, Giáo viên vắng/hủy, Gián đoạn, Hủy hệ thống (buổi bị hủy tự động do đổi giáo viên phụ trách, ngừng kích hoạt học viên hoặc ẩn danh hóa — không trừ buổi, không tính vào tỷ lệ hủy). Sau giờ học, giáo viên của buổi đánh dấu kết quả (Hoàn thành, Học viên vắng hoặc Gián đoạn); nếu sau 24 giờ chưa đánh dấu, hệ thống nhắc giáo viên và giáo vụ, giáo vụ được chốt thay. Buổi chưa được chốt kết quả chưa bị trừ khỏi gói. Buổi Gián đoạn không bị trừ trừ khi giáo vụ quyết định tính là buổi đã học (buổi chuyển sang Hoàn thành); nếu giáo vụ chọn xếp bù, buổi giữ trạng thái Gián đoạn và học viên đặt buổi mới.

**Hồ sơ năng lực & lộ trình (nhu cầu 3)**

- **FR-015**: Mỗi học viên MUST có hồ sơ năng lực gồm trình độ theo từng kỹ năng (Nghe, Nói, Đọc, Viết, Ngữ pháp/Từ vựng) quy về thang CEFR, và mục tiêu học tập tùy chọn (loại mục tiêu, mức cần đạt, thời hạn). Học viên chưa có mục tiêu MUST được hiển thị là "Cần tư vấn" (lọc được trong danh sách học viên) để giáo vụ hoặc giáo viên phụ trách tư vấn và ghi mục tiêu sau.
- **FR-016**: Giáo viên phụ trách MUST ghi nhận kết quả kiểm tra đầu vào chính thức (nguồn chính của hồ sơ năng lực, thay cho trình độ sơ bộ giáo vụ nhập lúc tạo hồ sơ) và các kỳ đánh giá định kỳ; mục tiêu học tập do giáo vụ hoặc giáo viên phụ trách sửa; hồ sơ năng lực và mục tiêu lưu lịch sử thay đổi theo thời gian.
- **FR-017**: Giáo viên MUST tạo lộ trình cá nhân cho học viên gồm các chặng, mỗi chặng có các chủ đề/mục tiêu bài học và mốc thời gian dự kiến. [GĐ2]: khởi tạo từ mẫu lộ trình do giáo vụ quản lý.
- **FR-018**: Giáo viên MUST điều chỉnh lộ trình (thêm/bớt/sắp xếp chủ đề, đổi mốc thời gian) kèm lý do; hệ thống lưu lịch sử điều chỉnh.

**Theo dõi kết quả & tiến bộ (nhu cầu 4)**

- **FR-019**: Sau mỗi buổi học, giáo viên MUST nộp phiếu đánh giá gồm: chủ đề lộ trình đã dạy, điểm kỹ năng trong buổi (thang 1–5), nhận xét, bài tập về nhà. Hệ thống nhắc nếu chưa nộp sau 24 giờ.
- **FR-020**: Học viên MUST nộp bài tập dưới dạng văn bản ([GĐ2]: tệp đính kèm, kể cả tệp âm thanh; mỗi tệp ≤ 20 MB, định dạng pdf, doc/docx, jpg, png, mp3, m4a, wav, webm); giáo viên chấm điểm (thang 0–10) và phản hồi. Bài tập có hạn nộp tùy chọn do giáo viên đặt; nộp sau hạn vẫn được nhận nhưng đánh dấu trễ. [GĐ2]: ghi âm trực tiếp trên web.
- **FR-021**: Học viên, giáo viên phụ trách và giáo vụ MUST xem được trang tiến bộ: lịch sử buổi học, nhận xét, diễn biến điểm kỹ năng theo thời gian, tỷ lệ hoàn thành lộ trình. [GĐ2]: tỷ lệ chuyên cần và tỷ lệ nộp bài tập.
- **FR-022**: Học viên MUST đánh giá được từng buổi học đã hoàn thành (1–5 sao, nhận xét tùy chọn) trong vòng 7 ngày sau buổi; đánh giá không sửa được sau khi gửi. Trong MVP chỉ giáo vụ xem đánh giá từng buổi; giáo viên không xem.
- **FR-023**: [GĐ2 — ngoài demo MVP] Giáo vụ MUST xem báo cáo chất lượng giảng dạy theo giáo viên và khoảng thời gian: điểm hài lòng trung bình (trung bình sao), số buổi dạy (theo định nghĩa "buổi đã dạy" ở FR-027), tỷ lệ hủy/vắng tách phía giáo viên (Giáo viên vắng/hủy) và phía học viên (Học viên vắng + Hủy muộn) trên mẫu số là số buổi đã đặt trong kỳ, loại bỏ khỏi mẫu số các buổi Hủy đúng hạn và Hủy hệ thống, tỷ lệ nộp phiếu đánh giá đúng hạn (phiếu nộp trong 24 giờ sau buổi / số buổi Hoàn thành). [GĐ2]: tỷ lệ đúng giờ, mức tiến bộ trung bình của học viên.

**Số buổi & học phí (nhu cầu 5)**

- **FR-024**: Hệ thống MUST tự động cập nhật số buổi đã học/đã đặt/còn lại theo trạng thái buổi học (buổi Đã đặt giữ chỗ ngay; còn lại = tổng − đã học − đã đặt; không cho đặt vượt số còn lại; hủy đúng hạn, Giáo viên vắng/hủy, Hủy hệ thống hoặc Gián đoạn thì trả lại chỗ). Giáo vụ MUST gia hạn thủ công được ghi danh đã hết hạn bằng cách đặt ngày hết hạn mới; hệ thống ghi lại người thực hiện và thời điểm và chặn đặt lịch khi gói chưa kích hoạt ("Chờ thanh toán"), hết buổi hoặc hết hạn.
- **FR-025**: Giáo vụ MUST ghi nhận thanh toán học phí (số tiền, ngày, phương thức, ghi chú) và xem công nợ theo học viên. Công nợ của một ghi danh = học phí của ghi danh (mặc định sao từ giá gói; giáo vụ chỉ nhập giá khác — ví dụ giảm giá — lúc tạo ghi danh, không sửa sau; sửa giá gói không ảnh hưởng ghi danh đã có) − tổng đã thanh toán. Học viên xem được (chỉ đọc) lịch sử thanh toán và công nợ của mình. Khoản thanh toán đầu tiên của một ghi danh kích hoạt gói (ngày kích hoạt là mốc tính hạn sử dụng); thanh toán chưa đủ học phí vẫn kích hoạt, phần còn thiếu là công nợ. Trong MVP, thanh toán được ghi nhận thủ công (chuyển khoản, tiền mặt); thanh toán trực tuyến qua cổng thanh toán thuộc giai đoạn 2.
- **FR-026**: Hệ thống MUST nhắc học viên và giáo vụ (qua email và thông báo trong hệ thống) khi gói còn ≤ 2 buổi hoặc còn ≤ 7 ngày đến hạn; mỗi lý do chỉ nhắc một lần cho mỗi ghi danh.
- **FR-027**: Giáo vụ MUST xem báo cáo tổng hợp: doanh thu đã thu, công nợ, số buổi đã dạy theo giáo viên (phục vụ tính thù lao giáo viên) trong khoảng thời gian chọn. "Buổi đã dạy" là mọi buổi mà học viên bị trừ: Hoàn thành, Học viên vắng, Hủy muộn, và Gián đoạn được giáo vụ tính là đã học; buổi Giáo viên vắng/hủy, Hủy đúng hạn, Hủy hệ thống và Gián đoạn được xếp bù không được tính.

**Chuẩn bị cho AI (nhu cầu 6 – giai đoạn sau)**

- **FR-028**: Dữ liệu năng lực, điểm kỹ năng từng buổi, nhận xét, kết quả bài tập và lịch sử lộ trình MUST được lưu dưới dạng có cấu trúc, xuất được, để phục vụ các tính năng AI giai đoạn sau. MVP không bao gồm tính năng AI.

**Khả năng thay thế dịch vụ bên ngoài**

- **FR-029**: Các dịch vụ bên ngoài (phòng học video, gửi email, lưu trữ tệp, thanh toán, AI) MUST thay thế được bằng nhà cung cấp khác chỉ qua cấu hình, không thay đổi nghiệp vụ và không mất dữ liệu.
- **FR-030**: Mọi người dùng MUST đồng ý điều khoản sử dụng và chính sách xử lý dữ liệu cá nhân khi đặt mật khẩu lần đầu; hệ thống lưu phiên bản chính sách và thời điểm đồng ý, và yêu cầu đồng ý lại khi chính sách đổi phiên bản. Người dùng chưa đồng ý phiên bản hiện hành chỉ thấy màn hình đồng ý sau khi đăng nhập và không dùng được chức năng khác; phiên bản chính sách được phát hành qua cấu hình triển khai. Ngày sinh là bắt buộc với học viên. Với học viên dưới 16 tuổi (tính theo ngày sinh tại thời điểm kích hoạt tài khoản), ngoài việc học viên tự đồng ý khi đặt mật khẩu, giáo vụ MUST ghi nhận đồng ý của cha mẹ/người giám hộ (họ tên, quan hệ, liên hệ, ngày đồng ý, người ghi nhận) trước khi tài khoản học viên được kích hoạt.
- **FR-031**: Giáo vụ MUST ghi nhận yêu cầu của chủ thể dữ liệu (người yêu cầu, ngày nhận, loại: xóa hoặc xem, ngày xử lý, kết quả, người xử lý). Với yêu cầu xóa hợp lệ, giáo vụ MUST ẩn danh hóa được hồ sơ học viên: xóa không khôi phục được họ tên, liên hệ, ngày sinh, thông tin đồng ý của người giám hộ, hồ sơ năng lực, mục tiêu, lộ trình, điểm kỹ năng và nhận xét trong phiếu đánh giá, bài tập và bài nộp (nội dung, tệp, điểm, phản hồi), nhận xét kèm đánh giá sao; giữ lại dưới dạng không định danh buổi học (thời gian, giáo viên, trạng thái), thời điểm nộp phiếu đánh giá, số sao, ghi danh, thanh toán và công nợ để số liệu tài chính và thù lao vẫn khớp (SC-007). Hồ sơ đã ẩn danh hóa bị ngừng kích hoạt vĩnh viễn; các buổi Đã đặt còn lại chuyển Hủy hệ thống. Bản ghi yêu cầu xóa (người yêu cầu, ngày, kết quả) được giữ làm bằng chứng tuân thủ. Với yêu cầu xem, giáo vụ MUST mở được trang tóm tắt in được (lưu PDF qua trình duyệt) liệt kê dữ liệu cá nhân đang lưu về học viên đó.

### Key Entities *(include if feature involves data)*

- **Người dùng (User)**: tài khoản đăng nhập, có một vai trò (Giáo vụ, Giáo viên, Học viên).
- **Yêu cầu của chủ thể dữ liệu (Data subject request)**: học viên liên quan, người yêu cầu, loại (xóa/xem), ngày nhận, ngày xử lý, kết quả, người xử lý.
- **Đồng ý xử lý dữ liệu (Consent)**: ai đồng ý (người dùng, hoặc cha mẹ/người giám hộ thay cho học viên dưới 16 tuổi), phiên bản chính sách, thời điểm, người ghi nhận.
- **Học viên (Student)**: thông tin cá nhân (gồm ngày sinh, bắt buộc), liên hệ, múi giờ, trạng thái; liên kết với hồ sơ năng lực, mục tiêu, lộ trình, ghi danh.
- **Giáo viên (Teacher)**: chuyên môn, chứng chỉ, lịch rảnh, học viên được phân công.
- **Khóa học (Course)**: chương trình học (ví dụ IELTS, Giao tiếp), có thể có mẫu lộ trình.
- **Gói học (Package)**: số buổi, thời lượng buổi, thời hạn, giá.
- **Ghi danh (Enrollment)**: học viên + gói học + giáo viên phụ trách; trạng thái (Chờ thanh toán → Đang dùng khi có thanh toán đầu tiên; Đang dùng → Hết buổi hoặc Hết hạn; Hết hạn → Đang dùng khi giáo vụ gia hạn; Chờ thanh toán/Đang dùng → Đã hủy); số buổi đã học/đã đặt/còn lại, ngày kích hoạt/hết hạn.
- **Lịch rảnh (Availability)**: khung giờ định kỳ và ngoại lệ của giáo viên.
- **Buổi học (Session)**: thời gian, giáo viên, học viên, ghi danh, trạng thái, lối vào phòng học.
- **Hồ sơ năng lực (Proficiency Profile)**: trình độ từng kỹ năng theo CEFR, có lịch sử theo thời gian.
- **Mục tiêu học tập (Learning Goal)**: loại mục tiêu, mức cần đạt, thời hạn; tùy chọn (học viên không có mục tiêu đang hiệu lực = "Cần tư vấn").
- **Lộ trình (Learning Path)**: các chặng (Milestone) → chủ đề bài học (Topic), mốc dự kiến, trạng thái hoàn thành, lịch sử điều chỉnh.
- **Phiếu đánh giá buổi học (Session Report)**: chủ đề đã dạy, điểm kỹ năng, nhận xét, bài tập giao.
- **Bài tập (Assignment) / Bài nộp (Submission)**: yêu cầu, hạn nộp (tùy chọn), bài nộp, cờ nộp trễ, điểm (0–10), phản hồi.
- **Đánh giá của học viên (Session Feedback)**: số sao, nhận xét.
- **Thanh toán (Payment)**: số tiền, ngày, phương thức, gắn với ghi danh.
- **Thông báo (Notification)**: nhắc lịch, nhắc gia hạn, nhắc nộp đánh giá.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Học viên đặt được một buổi học trong vòng dưới 1 phút kể từ khi mở trang đặt lịch.
- **SC-002**: Thời gian giáo vụ dành cho xếp lịch và nhắc lịch thủ công giảm ít nhất 70% so với trước khi dùng hệ thống.
- **SC-003**: 0 trường hợp trùng lịch giáo viên hoặc học viên.
- **SC-004**: Tỷ lệ học viên vắng không báo giảm ít nhất 30% sau 3 tháng nhờ nhắc lịch tự động.
- **SC-005**: Ít nhất 95% buổi học hoàn thành có phiếu đánh giá của giáo viên trong vòng 24 giờ.
- **SC-006**: 100% học viên đang học có hồ sơ năng lực và lộ trình cá nhân, và có mục tiêu hoặc đang ở trạng thái "Cần tư vấn", trong vòng 1 tuần sau khi ghi danh.
- **SC-007**: Số buổi còn lại và công nợ hiển thị chính xác 100% khi đối chiếu với lịch sử buổi học và thanh toán.
- **SC-008**: Giáo viên hoàn thành phiếu đánh giá một buổi học trong dưới 3 phút.
- **SC-009**: Ít nhất 80% học viên được khảo sát cho biết họ hiểu rõ mình đang ở đâu trên lộ trình và đã tiến bộ thế nào.
- **SC-010**: Sau ra mắt (khi dùng gói dịch vụ video trả phí), hệ thống phục vụ ổn định ít nhất 200 buổi học diễn ra đồng thời. Trong demo chỉ kiểm ở mức backend: xử lý được lượt vào phòng và thao tác của 200 buổi đồng thời, không đo chất lượng video.

## Assumptions

- Giao diện chính bằng tiếng Việt; nội dung học tập có thể bằng tiếng Anh. Hỗ trợ dùng tốt trên cả máy tính và điện thoại qua trình duyệt; ứng dụng di động riêng ngoài phạm vi MVP.
- Mỗi học viên có một giáo viên phụ trách chính trong một ghi danh; học viên chỉ đặt lịch với giáo viên phụ trách. Dạy thay thuộc [GĐ2]; trong MVP giáo viên vắng thì buổi chuyển Giáo viên vắng/hủy và giáo vụ xếp bù hoặc đổi giáo viên phụ trách.
- Thời lượng buổi học do gói học quy định (thường 25, 45 hoặc 60 phút).
- Thang đánh giá trình độ dùng CEFR (A1–C2); điểm từng buổi dùng thang 1–5. Thang quy đổi IELTS/TOEIC có thể bổ sung sau.
- Nhắc lịch qua email và thông báo trong hệ thống; nhắc qua SMS/Zalo là phần mở rộng sau.
- Hạn chót hủy mặc định 12 giờ, cấu hình được qua cấu hình triển khai (không có màn hình cấu hình trong MVP). Mốc nhắc lịch cố định 24h và 1h trong MVP.
- Thù lao giáo viên: MVP chỉ cung cấp báo cáo số buổi đã dạy; tính lương tự động ngoài phạm vi.
- Ghi hình buổi học ngoài phạm vi MVP.
- Giai đoạn demo dùng các gói miễn phí của dịch vụ bên ngoài. Khi lượng người dùng vượt hạn mức, chuyển sang gói trả phí hoặc đổi nhà cung cấp (xem FR-029).
- Không có tài khoản phụ huynh; học viên chủ yếu là người lớn. Với học viên dưới 16 tuổi, đồng ý của cha mẹ/người giám hộ do giáo vụ ghi nhận (FR-030). Mốc 16 tuổi và nội dung chính sách dữ liệu cần bộ phận pháp lý của trung tâm xác nhận.
- Dữ liệu cá nhân được bảo vệ theo quy định pháp luật Việt Nam về bảo vệ dữ liệu cá nhân; chỉ người có quyền mới truy cập được.
- Tính năng AI (nhu cầu 6) ngoài phạm vi MVP; MVP chỉ đảm bảo dữ liệu đủ cấu trúc để khai thác sau.
