import { useState } from 'react'
import { useAuth } from '@/app/AuthProvider'
import { errorMessage } from '@/components/useApiErrorToast'
import { Button } from '@/components/ui/button'
import { authApi } from '@/lib/api/auth'
import { AuthCard } from './AuthCard'

/**
 * Khi chính sách đổi phiên bản, người dùng chỉ thấy màn này cho tới khi đồng ý (FR-030). Backend
 * cũng chặn mọi API khác bằng CONSENT_REQUIRED.
 */
export function ConsentGatePage() {
  const { me, reloadMe, logout } = useAuth()
  const [accepted, setAccepted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  if (!me) return null

  const accept = async () => {
    setBusy(true)
    setError(null)
    try {
      await authApi.acceptPolicy(me.currentPolicyVersion)
      await reloadMe()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthCard
      title="Cập nhật điều khoản và chính sách dữ liệu"
      subtitle={`Phiên bản ${me.currentPolicyVersion}`}
    >
      <p className="text-sm">
        Trung tâm đã cập nhật Điều khoản sử dụng và Chính sách xử lý dữ liệu cá nhân. Bạn cần đồng ý
        phiên bản mới để tiếp tục sử dụng hệ thống.
      </p>
      <label className="flex items-start gap-2 text-sm">
        <input
          type="checkbox"
          className="mt-1"
          checked={accepted}
          onChange={(e) => setAccepted(e.target.checked)}
        />
        <span>
          Tôi đã đọc và đồng ý với điều khoản và chính sách dữ liệu phiên bản{' '}
          {me.currentPolicyVersion}.
        </span>
      </label>
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
      <div className="flex gap-2">
        <Button onClick={() => void accept()} disabled={!accepted || busy} className="flex-1">
          Đồng ý và tiếp tục
        </Button>
        <Button variant="outline" onClick={() => void logout()}>
          Đăng xuất
        </Button>
      </div>
    </AuthCard>
  )
}
