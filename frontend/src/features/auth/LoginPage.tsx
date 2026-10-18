import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { useAuth } from '@/app/AuthProvider'
import { HOME_BY_ROLE } from '@/app/RequireRole'
import { errorMessage } from '@/components/useApiErrorToast'
import { Button } from '@/components/ui/button'
import { FieldError, Input, Label } from '@/components/ui/input'
import { AuthCard } from './AuthCard'

const schema = z.object({
  email: z.string().trim().min(1, 'Nhập email').email('Email không hợp lệ'),
  password: z.string().min(1, 'Nhập mật khẩu'),
})
type FormValues = z.infer<typeof schema>

export default function LoginPage() {
  const { status, me, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  if (status === 'authenticated' && me) return <Navigate to={HOME_BY_ROLE[me.role]} replace />

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    try {
      const current = await login(values.email, values.password)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from ?? HOME_BY_ROLE[current.role], { replace: true })
    } catch (error) {
      setServerError(errorMessage(error))
    }
  })

  return (
    <AuthCard title="Đăng nhập" subtitle="Dành cho giáo vụ, giáo viên và học viên">
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
        <div className="space-y-1.5">
          <Label htmlFor="password">Mật khẩu</Label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            aria-invalid={!!errors.password}
            {...register('password')}
          />
          <FieldError message={errors.password?.message} />
        </div>
        {serverError && (
          <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-destructive">
            {serverError}
          </p>
        )}
        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </Button>
        <p className="text-center text-sm">
          <Link to="/forgot-password" className="text-primary hover:underline">
            Quên mật khẩu?
          </Link>
        </p>
      </form>
    </AuthCard>
  )
}
