export const SKILL_SERIES = [
  { key: 'listening', label: 'Nghe' },
  { key: 'speaking', label: 'Nói' },
  { key: 'reading', label: 'Đọc' },
  { key: 'writing', label: 'Viết' },
  { key: 'grammarVocab', label: 'Ngữ pháp & Từ vựng' },
] as const

/** `date` là ngày YYYY-MM-DD đã tính theo múi giờ nghiệp vụ, không đổi múi giờ lần nữa. */
export function shortDate(date: string) {
  const [, month, day] = date.split('-')
  return month && day ? `${day}/${month}` : date
}
