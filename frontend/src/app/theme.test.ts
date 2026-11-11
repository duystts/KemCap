import { afterEach, describe, expect, it, vi } from 'vitest'
import { getThemeChoice, setThemeChoice } from './theme'

function mockSystemDark(matches: boolean) {
  vi.stubGlobal(
    'matchMedia',
    vi.fn(() => ({ matches, addEventListener: vi.fn(), removeEventListener: vi.fn() })),
  )
}

describe('theme', () => {
  afterEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
    vi.unstubAllGlobals()
  })

  it('mặc định là sáng khi chưa chọn', () => {
    expect(getThemeChoice()).toBe('light')
  })

  it('bỏ qua giá trị lạ trong localStorage', () => {
    localStorage.setItem('theme', 'neon')
    expect(getThemeChoice()).toBe('light')
  })

  it('chọn tối thì lưu lại và đặt lớp dark trên html', () => {
    setThemeChoice('dark')
    expect(localStorage.getItem('theme')).toBe('dark')
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    setThemeChoice('light')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('theo thiết bị thì lấy theo prefers-color-scheme', () => {
    mockSystemDark(true)
    setThemeChoice('system')
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    mockSystemDark(false)
    setThemeChoice('system')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })
})
