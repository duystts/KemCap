import { cn } from '@/lib/utils'
import { setThemeChoice, useThemeChoice, type ThemeChoice } from './theme'

const OPTIONS: { value: ThemeChoice; label: string }[] = [
  { value: 'light', label: 'Sáng' },
  { value: 'dark', label: 'Tối' },
  { value: 'system', label: 'Theo thiết bị' },
]

export function ThemeSelect({ className }: { className?: string }) {
  const choice = useThemeChoice()
  return (
    <select
      aria-label="Giao diện"
      value={choice}
      onChange={(e) => setThemeChoice(e.target.value as ThemeChoice)}
      className={cn(
        'h-8 rounded-[var(--radius)] border border-border bg-white px-2 text-sm outline-none focus-visible:border-primary',
        className,
      )}
    >
      {OPTIONS.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  )
}
