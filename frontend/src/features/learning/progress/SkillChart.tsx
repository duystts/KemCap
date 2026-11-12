import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { SkillPoint } from '@/lib/api/learning'
import { SKILL_SERIES, shortDate } from './skills'

type SkillKey = (typeof SKILL_SERIES)[number]['key']

interface TooltipEntry {
  value?: unknown
  payload?: SkillPoint
}

// Recharts ghi đè prop `label` của nội dung tooltip bằng giá trị trục X
function ChartTooltip({
  active,
  payload,
  skillLabel,
}: {
  active?: boolean
  payload?: readonly TooltipEntry[]
  skillLabel: string
}) {
  const entry = payload?.[0]
  if (!active || !entry?.payload) return null
  return (
    <div className="rounded-lg border bg-white px-3 py-2 text-xs shadow-sm">
      <p className="font-semibold text-slate-800">{shortDate(entry.payload.date)}</p>
      <p className="text-slate-600">
        {skillLabel}: <span className="font-semibold text-slate-900">{String(entry.value)}</span>
      </p>
    </div>
  )
}

function SkillPanel({
  skill,
  label,
  points,
}: {
  skill: SkillKey
  label: string
  points: SkillPoint[]
}) {
  const scored = points.filter((p) => p[skill] != null)
  const latest = scored[scored.length - 1]?.[skill]
  const dateOf = new Map(points.map((p) => [p.sessionId, p.date]))

  return (
    <figure className="min-w-0">
      <figcaption className="flex items-baseline justify-between gap-2 mb-1">
        <span className="text-sm font-medium text-slate-800">{label}</span>
        <span className="text-xs text-slate-500">
          {latest == null ? (
            'Chưa chấm'
          ) : (
            <>
              Gần nhất <span className="font-semibold text-slate-900">{latest}</span>
            </>
          )}
        </span>
      </figcaption>
      <div className="h-40 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={points} margin={{ top: 8, right: 12, bottom: 0, left: 0 }}>
            <CartesianGrid vertical={false} stroke="var(--color-slate-200)" />
            {/* Nhiều buổi có thể cùng ngày nên trục theo buổi, nhãn là ngày */}
            <XAxis
              dataKey="sessionId"
              tickFormatter={(id: string) => shortDate(dateOf.get(id) ?? '')}
              tick={{ fill: 'var(--color-slate-500)', fontSize: 11 }}
              tickLine={false}
              axisLine={{ stroke: 'var(--color-slate-300)' }}
              minTickGap={16}
            />
            <YAxis
              domain={[1, 5]}
              ticks={[1, 2, 3, 4, 5]}
              width={20}
              tick={{ fill: 'var(--color-slate-500)', fontSize: 11 }}
              tickLine={false}
              axisLine={false}
            />
            <Tooltip
              content={<ChartTooltip skillLabel={label} />}
              cursor={{ stroke: 'var(--color-slate-400)' }}
            />
            <Line
              type="linear"
              dataKey={skill}
              name={label}
              stroke="var(--color-primary)"
              strokeWidth={2}
              dot={{
                r: 4,
                strokeWidth: 2,
                stroke: 'var(--color-white)',
                fill: 'var(--color-primary)',
              }}
              activeDot={{
                r: 5,
                strokeWidth: 2,
                stroke: 'var(--color-white)',
                fill: 'var(--color-primary)',
              }}
              connectNulls
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </figure>
  )
}

/**
 * Mỗi kỹ năng một ô, cùng thang 1–5. Điểm chỉ có 5 mức nên vẽ chung một biểu đồ thì các đường
 * trùng nhau và che mất kỹ năng. Điểm bỏ trống thì đường nối qua, không tính là 0.
 */
export default function SkillChart({ points }: { points: SkillPoint[] }) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-5">
      {SKILL_SERIES.map((s) => (
        <SkillPanel key={s.key} skill={s.key} label={s.label} points={points} />
      ))}
    </div>
  )
}
