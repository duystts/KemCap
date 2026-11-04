import { api } from './client'

export type PaymentMethod = 'BANK_TRANSFER' | 'CASH' | 'OTHER'

export interface PaymentItem {
  id: string
  amountVnd: number
  paidAt: string // YYYY-MM-DD
  method: PaymentMethod
  adjustment: boolean
  note?: string | null
  recordedBy: string
  recordedAt: string
}

export interface StatementDto {
  enrollmentId: string
  amountDueVnd: number
  amountPaidVnd: number
  debtVnd: number
  payments: PaymentItem[]
}

export function fetchStatement(enrollmentId: string): Promise<StatementDto> {
  return api<StatementDto>(`/enrollments/${enrollmentId}/payments`)
}

export function recordPayment(
  enrollmentId: string,
  data: {
    amountVnd: number
    paidAt: string
    method: PaymentMethod
    note?: string
  },
): Promise<PaymentItem> {
  return api<PaymentItem>(`/enrollments/${enrollmentId}/payments`, {
    method: 'POST',
    body: data,
  })
}

export function recordAdjustment(
  enrollmentId: string,
  data: {
    amountVnd: number
    note: string
  },
): Promise<PaymentItem> {
  return api<PaymentItem>(`/enrollments/${enrollmentId}/payments/adjustments`, {
    method: 'POST',
    body: data,
  })
}
