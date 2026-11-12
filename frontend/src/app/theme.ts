import { useSyncExternalStore } from 'react'

export type ThemeChoice = 'light' | 'dark' | 'system'
export type ResolvedTheme = 'light' | 'dark'

// Cùng khóa với đoạn script trong index.html (đặt lớp `dark` trước khi React vẽ, tránh nháy màu)
const STORAGE_KEY = 'theme'
const DARK_QUERY = '(prefers-color-scheme: dark)'

const listeners = new Set<() => void>()

function systemPrefersDark(): boolean {
  return typeof window.matchMedia === 'function' && window.matchMedia(DARK_QUERY).matches
}

export function getThemeChoice(): ThemeChoice {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored === 'dark' || stored === 'system') return stored
  } catch {
    // Trình duyệt chặn localStorage: dùng mặc định
  }
  return 'light'
}

function resolve(choice: ThemeChoice): ResolvedTheme {
  if (choice === 'system') return systemPrefersDark() ? 'dark' : 'light'
  return choice
}

function apply() {
  document.documentElement.classList.toggle('dark', resolve(getThemeChoice()) === 'dark')
  listeners.forEach((notify) => notify())
}

export function setThemeChoice(choice: ThemeChoice) {
  try {
    localStorage.setItem(STORAGE_KEY, choice)
  } catch {
    // Không lưu được thì vẫn đổi cho lần xem này
    document.documentElement.classList.toggle('dark', resolve(choice) === 'dark')
    listeners.forEach((notify) => notify())
    return
  }
  apply()
}

/** Gọi một lần khi khởi động: áp theme đã lưu và theo dõi thay đổi từ hệ điều hành, tab khác. */
export function initTheme() {
  apply()
  if (typeof window.matchMedia === 'function') {
    window.matchMedia(DARK_QUERY).addEventListener('change', apply)
  }
  window.addEventListener('storage', (event) => {
    if (event.key === STORAGE_KEY) apply()
  })
}

function subscribe(onChange: () => void) {
  listeners.add(onChange)
  return () => {
    listeners.delete(onChange)
  }
}

export function useThemeChoice(): ThemeChoice {
  return useSyncExternalStore(subscribe, getThemeChoice, () => 'light')
}

export function useResolvedTheme(): ResolvedTheme {
  return useSyncExternalStore(
    subscribe,
    () => (document.documentElement.classList.contains('dark') ? 'dark' : 'light'),
    () => 'light',
  )
}
