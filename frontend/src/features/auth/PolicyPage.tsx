import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { authApi } from '@/lib/api/auth'
import { AuthCard } from './AuthCard'

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-1.5">
      <h2 className="font-semibold">{title}</h2>
      {children}
    </section>
  )
}

/**
 * Điều khoản sử dụng và chính sách xử lý dữ liệu cá nhân (FR-030, R17 của U1). Trang công khai để
 * mở được từ màn kích hoạt tài khoản và màn đồng ý lại. Nội dung chính thức chờ pháp lý trung tâm.
 */
export default function PolicyPage() {
  const policy = useQuery({ queryKey: ['auth', 'policy'], queryFn: authApi.policy })
  return (
    <AuthCard
      title="Điều khoản sử dụng và Chính sách xử lý dữ liệu cá nhân"
      subtitle={policy.data ? `Phiên bản ${policy.data.currentPolicyVersion}` : undefined}
    >
      <div className="space-y-4 text-sm leading-relaxed">
        <p className="rounded-md border border-border bg-muted p-3">
          Đây là bản tóm tắt. Nội dung chính thức đang chờ bộ phận pháp lý của trung tâm duyệt; khi
          có bản mới, bạn sẽ được yêu cầu đồng ý lại.
        </p>
        <Section title="Dữ liệu được lưu">
          <ul className="list-disc space-y-1 pl-5">
            <li>Họ tên, email, số điện thoại; ngày sinh của học viên.</li>
            <li>
              Với học viên dưới 16 tuổi: thông tin đồng ý của cha mẹ hoặc người giám hộ (họ tên,
              quan hệ, liên hệ, ngày đồng ý).
            </li>
            <li>
              Hồ sơ năng lực, mục tiêu, lộ trình, lịch học, phiếu đánh giá sau buổi, bài tập và bài
              nộp, đánh giá sao.
            </li>
            <li>Gói học đã đăng ký, các khoản đã thu và công nợ.</li>
          </ul>
        </Section>
        <Section title="Mục đích">
          <p>
            Tổ chức buổi học 1 kèm 1, theo dõi tiến bộ, gửi email nhắc lịch và thông báo, quản lý
            học phí. Hệ thống không ghi hình buổi học.
          </p>
        </Section>
        <Section title="Ai xem được">
          <p>
            Học viên xem dữ liệu của mình. Giáo viên chỉ xem học viên mình đang dạy. Giáo vụ của
            trung tâm xem để vận hành. Dịch vụ phòng học video và dịch vụ gửi email nhận phần dữ
            liệu cần thiết để hoạt động.
          </p>
        </Section>
        <Section title="Quyền của bạn">
          <p>
            Bạn có thể yêu cầu giáo vụ cho xem toàn bộ dữ liệu cá nhân trung tâm đang lưu về bạn,
            hoặc yêu cầu xóa. Khi xóa, thông tin định danh và nội dung học tập bị xóa không khôi
            phục được; lịch sử buổi học, gói học và thanh toán được giữ ở dạng không định danh để số
            liệu tài chính vẫn khớp.
          </p>
        </Section>
      </div>
    </AuthCard>
  )
}
