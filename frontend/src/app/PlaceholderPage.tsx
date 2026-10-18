import { EmptyState } from '@/components/EmptyState'

/** Trang giữ chỗ cho các màn hình làm ở unit sau. */
export default function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold">{title}</h1>
      <EmptyState title="Đang xây dựng" description="Màn hình này sẽ có ở các bản tiếp theo." />
    </div>
  )
}
