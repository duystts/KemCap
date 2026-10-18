import type { ReactNode } from 'react'
import { Button } from './ui/button'
import { EmptyState } from './EmptyState'

export interface Column<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  /** tên field gửi trong `?sort=field,asc` nếu cột sort được */
  sortField?: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface SortState {
  field: string
  direction: 'asc' | 'desc'
}

/** Bảng phân trang/sort theo quy ước `?page&size&sort` của contracts/rest-api.md. */
export function DataTable<T>({
  columns,
  page,
  rowKey,
  sort,
  onSortChange,
  onPageChange,
  emptyTitle = 'Chưa có dữ liệu',
  loading,
}: {
  columns: Column<T>[]
  page: Page<T> | undefined
  rowKey: (row: T) => string
  sort?: SortState
  onSortChange?: (sort: SortState) => void
  onPageChange?: (page: number) => void
  emptyTitle?: string
  loading?: boolean
}) {
  if (loading && !page) return <p className="p-4 text-sm text-muted-foreground">Đang tải…</p>
  if (!page || page.content.length === 0) return <EmptyState title={emptyTitle} />

  const toggleSort = (field: string) => {
    if (!onSortChange) return
    const direction = sort?.field === field && sort.direction === 'asc' ? 'desc' : 'asc'
    onSortChange({ field, direction })
  }

  return (
    <div className="space-y-3">
      <div className="overflow-x-auto rounded-xl border border-border">
        <table className="w-full text-left text-sm">
          <thead className="bg-muted text-xs uppercase text-muted-foreground">
            <tr>
              {columns.map((col) => (
                <th key={col.key} className="px-4 py-3 font-medium">
                  {col.sortField ? (
                    <button type="button" onClick={() => toggleSort(col.sortField!)}>
                      {col.header}
                      {sort?.field === col.sortField && (sort.direction === 'asc' ? ' ▲' : ' ▼')}
                    </button>
                  ) : (
                    col.header
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {page.content.map((row) => (
              <tr key={rowKey(row)} className="border-t border-border">
                {columns.map((col) => (
                  <td key={col.key} className="px-4 py-3">
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {page.totalPages > 1 && (
        <div className="flex items-center justify-end gap-2 text-sm">
          <Button
            variant="outline"
            size="sm"
            disabled={page.page === 0}
            onClick={() => onPageChange?.(page.page - 1)}
          >
            Trước
          </Button>
          <span>
            Trang {page.page + 1}/{page.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page.page + 1 >= page.totalPages}
            onClick={() => onPageChange?.(page.page + 1)}
          >
            Sau
          </Button>
        </div>
      )}
    </div>
  )
}
