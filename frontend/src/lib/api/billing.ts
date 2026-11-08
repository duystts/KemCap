import { api } from './client'

export type PaymentMethod = 'BANK_TRANSFER' | 'CASH' | 'OTHER'

export const PAYMENT_METHOD_LABEL: Record<PaymentMethod, string> = {
  BANK_TRANSFER: 'Chuyển khoản',
  CASH: 'Tiền mặt',
  OTHER: 'Khác',
}

/** Ghi chú thanh toán là văn bản tự do; gợi ý này là biện pháp duy nhất tránh ghi dữ liệu cá nhân vào đó (research.md). */
export const PAYMENT_NOTE_HINT =
  'Không ghi tên, số điện thoại hay thông tin cá nhân của học viên. Chỉ ghi mã giao dịch hoặc nơi thu.'

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

export interface ReceivableItem {
  enrollmentId: string
  studentId: string
  studentName?: string | null
  amountDueVnd: number
  amountPaidVnd: number
  debtVnd: number
  cancelled: boolean
}

export function fetchReceivables(params?: { hasDebt?: boolean }): Promise<ReceivableItem[]> {
  const query = params?.hasDebt === undefined ? '' : `?hasDebt=${params.hasDebt}`
  return api<ReceivableItem[]>(`/receivables${query}`)
}

export interface RevenueReport {
  from?: string | null
  to?: string | null
  collectedVnd: number
  debtVnd: number
}

export function fetchRevenue(range: { from: string; to: string }): Promise<RevenueReport> {
  return api<RevenueReport>(`/reports/revenue?from=${range.from}&to=${range.to}`)
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
