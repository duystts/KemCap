import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router'
import { z } from 'zod'
import { errorMessage } from '@/components/useApiErrorToast'
import { Button } from '@/components/ui/button'
import { FieldError, Input, Label } from '@/components/ui/input'
import { authApi } from '@/lib/api/auth'
import { AuthCard, passwordSchema } from './AuthCard'

const schema = z
  .object({
    password: passwordSchema,
    confirm: z.string(),
    accepted: z.boolean().refine((v) => v, 'Bạn cần đồng ý điều khoản và chính sách dữ liệu'),
  })
  .refine((v) => v.password === v.confirm, {
    path: ['confirm'],
    message: 'Mật khẩu nhập lại không khớp',
  })
type FormValues = z.infer<typeof schema>

/** Đặt mật khẩu lần đầu + đồng ý chính sách hiện hành (FR-002, FR-030). */
export default function AcceptInvitePage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [done, setDone] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const policy = useQuery({ queryKey: ['auth', 'policy'], queryFn: authApi.policy })
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { accepted: false } })

  if (!token) {
    return (
      <AuthCard title="Link mời không hợp lệ">
        <p className="text-sm">Vui lòng mở đúng link trong email mời hoặc liên hệ giáo vụ.</p>
      </AuthCard>
    )
  }

  if (done) {
    return (
      <AuthCard title="Kích hoạt thành công">
        <p className="text-sm">Tài khoản đã sẵn sàng. Bạn có thể đăng nhập ngay.</p>
        <Link to="/login" className="text-sm text-primary hover:underline">
          Đi tới đăng nhập
        </Link>
      </AuthCard>
    )
  }

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    if (!policy.data) return
    try {
      await authApi.acceptInvite(token, values.password, policy.data.currentPolicyVersion)
      setDone(true)
    } catch (error) {
      setServerError(errorMessage(error))
    }
  })

  return (
    <AuthCard title="Kích hoạt tài khoản" subtitle="Đặt mật khẩu để bắt đầu sử dụng">
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div className="space-y-1.5">
          <Label htmlFor="password">Mật khẩu mới</Label>
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            aria-invalid={!!errors.password}
            {...register('password')}
          />
          <FieldError message={errors.password?.message} />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="confirm">Nhập lại mật khẩu</Label>
          <Input
            id="confirm"
            type="password"
            autoComplete="new-password"
            aria-invalid={!!errors.confirm}
            {...register('confirm')}
          />
          <FieldError message={errors.confirm?.message} />
        </div>
        <div className="space-y-1.5">
          <label className="flex items-start gap-2 text-sm">
            <input type="checkbox" className="mt-1" {...register('accepted')} />
            <span>
              Tôi đồng ý với <strong>Điều khoản sử dụng và Chính sách xử lý dữ liệu cá nhân</strong>
              {policy.data && <> (phiên bản {policy.data.currentPolicyVersion})</>}.
            </span>
          </label>
          <FieldError message={errors.accepted?.message} />
        </div>
        {serverError && (
          <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-destructive">
            {serverError}
          </p>
        )}
        <Button type="submit" className="w-full" disabled={isSubmitting || !policy.data}>
          {isSubmitting ? 'Đang kích hoạt…' : 'Kích hoạt tài khoản'}
        </Button>
      </form>
    </AuthCard>
  )
}
