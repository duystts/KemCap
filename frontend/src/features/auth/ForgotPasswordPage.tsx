import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router'
import { z } from 'zod'
import { errorMessage } from '@/components/useApiErrorToast'
import { Button } from '@/components/ui/button'
import { FieldError, Input, Label } from '@/components/ui/input'
import { authApi } from '@/lib/api/auth'
import { AuthCard } from './AuthCard'

const schema = z.object({
  email: z.string().trim().min(1, 'Nhập email').email('Email không hợp lệ'),
})
type FormValues = z.infer<typeof schema>

export default function ForgotPasswordPage() {
  const [sent, setSent] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const onSubmit = handleSubmit(async ({ email }) => {
    setServerError(null)
    try {
      await authApi.forgotPassword(email)
      setSent(true)
    } catch (error) {
      setServerError(errorMessage(error))
    }
  })

  return (
    <AuthCard title="Quên mật khẩu" subtitle="Nhập email để nhận link đặt lại mật khẩu">
      {sent ? (
        <p className="text-sm">
          Nếu email tồn tại trong hệ thống, bạn sẽ nhận được link đặt lại mật khẩu trong vài phút.
          Link có hiệu lực 30 phút.
        </p>
      ) : (
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              aria-invalid={!!errors.email}
              {...register('email')}
            />
            <FieldError message={errors.email?.message} />
          </div>
          {serverError && (
            <p role="alert" className="text-sm text-destructive">
              {serverError}
            </p>
          )}
          <Button type="submit" className="w-full" disabled={isSubmitting}>
            Gửi link đặt lại
          </Button>
        </form>
      )}
      <p className="text-center text-sm">
        <Link to="/login" className="text-primary hover:underline">
          Quay lại đăng nhập
        </Link>
      </p>
    </AuthCard>
  )
}
