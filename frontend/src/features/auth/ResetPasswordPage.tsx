import { zodResolver } from '@hookform/resolvers/zod'
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
  .object({ newPassword: passwordSchema, confirm: z.string() })
  .refine((v) => v.newPassword === v.confirm, {
    path: ['confirm'],
    message: 'Mật khẩu nhập lại không khớp',
  })
type FormValues = z.infer<typeof schema>

export default function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [done, setDone] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const onSubmit = handleSubmit(async ({ newPassword }) => {
    setServerError(null)
    try {
      await authApi.resetPassword(token, newPassword)
      setDone(true)
    } catch (error) {
      setServerError(errorMessage(error))
    }
  })

  return (
    <AuthCard title="Đặt lại mật khẩu">
      {done ? (
        <p className="text-sm">
          Đã đổi mật khẩu.{' '}
          <Link to="/login" className="text-primary hover:underline">
            Đăng nhập
          </Link>
        </p>
      ) : (
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="newPassword">Mật khẩu mới</Label>
            <Input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors.newPassword}
              {...register('newPassword')}
            />
            <FieldError message={errors.newPassword?.message} />
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
          {serverError && (
            <p role="alert" className="text-sm text-destructive">
              {serverError}
            </p>
          )}
          <Button type="submit" className="w-full" disabled={isSubmitting || !token}>
            Đổi mật khẩu
          </Button>
        </form>
      )}
    </AuthCard>
  )
}
