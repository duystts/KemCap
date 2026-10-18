import { z } from 'zod'
import type { ReactNode } from 'react'
import { Card } from '@/components/ui/card'

export function AuthCard({
  title,
  subtitle,
  children,
}: {
  title: string
  subtitle?: string
  children: ReactNode
}) {
  return (
    <div className="flex min-h-full items-center justify-center bg-muted px-4 py-10">
      <Card className="w-full max-w-md space-y-6">
        <div className="space-y-1">
          <p className="text-sm font-semibold text-primary">English 1-1</p>
          <h1 className="text-2xl font-semibold">{title}</h1>
          {subtitle && <p className="text-sm text-muted-foreground">{subtitle}</p>}
        </div>
        {children}
      </Card>
    </div>
  )
}

export const PASSWORD_RULE = /^(?=.*\p{L})(?=.*\d).{8,}$/u
export const PASSWORD_MESSAGE = 'Mật khẩu tối thiểu 8 ký tự, có chữ và số'

/** Giống PasswordPolicy ở backend: BCrypt chỉ nhận tối đa 72 byte. */
export const passwordSchema = z
  .string()
  .regex(PASSWORD_RULE, PASSWORD_MESSAGE)
  .refine((v) => new TextEncoder().encode(v).length <= 72, 'Mật khẩu quá dài (tối đa 72 byte)')
