import type { ReportScores } from '@/lib/api/learning'

export const SKILLS: { key: keyof ReportScores; label: string }[] = [
  { key: 'listening', label: 'Nghe' },
  { key: 'speaking', label: 'Nói' },
  { key: 'reading', label: 'Đọc' },
  { key: 'writing', label: 'Viết' },
  { key: 'grammarVocab', label: 'Ngữ pháp & Từ vựng' },
]
