/// <reference types="vite/client" />

declare module 'lucide-react' {
  import * as React from 'react'
  export interface LucideProps extends React.SVGProps<SVGSVGElement> {
    size?: string | number
    color?: string
    strokeWidth?: string | number
  }
  export type LucideIcon = React.ForwardRefExoticComponent<
    LucideProps & React.RefAttributes<SVGSVGElement>
  >
  export const LogOut: LucideIcon
  export const Bell: LucideIcon
  export const CheckCheck: LucideIcon
  export const Eye: LucideIcon
  export const EyeOff: LucideIcon
  export const Calendar: LucideIcon
  export const Clock: LucideIcon
  export const User: LucideIcon
  export const Users: LucideIcon
  export const BookOpen: LucideIcon
  export const Video: LucideIcon
  export const Mic: LucideIcon
  export const MicOff: LucideIcon
  export const VideoOff: LucideIcon
  export const PhoneOff: LucideIcon
  export const Settings: LucideIcon
  export const Plus: LucideIcon
  export const Trash2: LucideIcon
  export const Edit: LucideIcon
  export const ChevronLeft: LucideIcon
  export const ChevronRight: LucideIcon
  export const Search: LucideIcon
  export const X: LucideIcon
  export const Check: LucideIcon
  export const AlertCircle: LucideIcon
  export const AlertTriangle: LucideIcon
  export const Info: LucideIcon
  export const ArrowLeft: LucideIcon
  export const Filter: LucideIcon
  export const ExternalLink: LucideIcon
  export const CheckCircle: LucideIcon
  export const CheckCircle2: LucideIcon
  export const XCircle: LucideIcon
  export const Mail: LucideIcon
  export const Shield: LucideIcon
  export const Award: LucideIcon
  export const FileText: LucideIcon
  export const DollarSign: LucideIcon
  export const CreditCard: LucideIcon
  export const Send: LucideIcon
  export const History: LucideIcon
  export const TrendingUp: LucideIcon
  export const Target: LucideIcon
  export const Book: LucideIcon
  export const GraduationCap: LucideIcon
  export const Sparkles: LucideIcon
  export const RefreshCw: LucideIcon
  export const UserPlus: LucideIcon
  export const RotateCw: LucideIcon
  export const Power: LucideIcon
  export const Package: LucideIcon
  export const UserCheck: LucideIcon
  export const ArrowRight: LucideIcon
  export const Briefcase: LucideIcon
  export const Save: LucideIcon
  export const Circle: LucideIcon
}
