/** Ngày theo giờ máy dạng YYYY-MM-DD; không dùng toISOString vì trước 7h sáng sẽ lệch về hôm trước. */
export function localDate(d: Date) {
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

export interface DateRange {
  from: string
  to: string
}

export function thisMonth(today: Date): DateRange {
  return {
    from: localDate(new Date(today.getFullYear(), today.getMonth(), 1)),
    to: localDate(today),
  }
}

export function lastMonth(today: Date): DateRange {
  return {
    from: localDate(new Date(today.getFullYear(), today.getMonth() - 1, 1)),
    to: localDate(new Date(today.getFullYear(), today.getMonth(), 0)),
  }
}

export function last30Days(today: Date): DateRange {
  return {
    from: localDate(new Date(today.getFullYear(), today.getMonth(), today.getDate() - 29)),
    to: localDate(today),
  }
}
