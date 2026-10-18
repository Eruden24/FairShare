import { cn } from '@/lib/utils'

export function Logo({ className, inverted }: { className?: string; inverted?: boolean }) {
  return (
    <span className={cn('inline-flex items-center gap-2 font-semibold tracking-tight', className)}>
      <svg viewBox="0 0 32 32" className="size-7" aria-hidden>
        <rect width="32" height="32" rx="8" className={inverted ? 'fill-white' : 'fill-primary'} />
        <path
          d="M9 22V10h10M9 16h7"
          className={inverted ? 'stroke-primary' : 'stroke-white'}
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
        />
        <circle cx="23" cy="21" r="3" className={inverted ? 'fill-primary/50' : 'fill-indigo-300'} />
      </svg>
      <span className="text-base">FairShare</span>
    </span>
  )
}
