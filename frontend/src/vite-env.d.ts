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
  export const Info: LucideIcon
}
