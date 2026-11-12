import { useEffect, useState } from 'react'

/** Trả về giá trị sau khi nó đứng yên `delayMs`; dùng cho ô tìm kiếm để không gọi API theo từng phím. */
export function useDebouncedValue<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])
  return debounced
}
