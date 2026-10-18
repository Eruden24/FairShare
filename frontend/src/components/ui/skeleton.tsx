import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

/** A grey placeholder shown while data loads, in the shape of what's coming */
export function Skeleton({ className, ...props }: ComponentProps<'div'>) {
  return <div className={cn('animate-pulse rounded-md bg-muted', className)} aria-hidden {...props} />
}
