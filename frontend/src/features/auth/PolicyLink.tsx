import type { ReactNode } from 'react'

/** Mở chính sách ở tab mới để không mất mật khẩu đang nhập ở màn kích hoạt. */
export function PolicyLink({ children }: { children: ReactNode }) {
  return (
    <a
      href="/policy"
      target="_blank"
      rel="noopener noreferrer"
      className="font-semibold text-primary underline underline-offset-2"
    >
      {children}
    </a>
  )
}
