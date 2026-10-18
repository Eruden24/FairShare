import { Avatar as AvatarPrimitive } from 'radix-ui'
import { cn, initials } from '@/lib/utils'

// A stable color per person, so the same name always gets the same avatar color
const palette = [
  'bg-indigo-100 text-indigo-700',
  'bg-emerald-100 text-emerald-700',
  'bg-amber-100 text-amber-800',
  'bg-rose-100 text-rose-700',
  'bg-sky-100 text-sky-700',
  'bg-violet-100 text-violet-700',
]

function colorFor(name: string) {
  let hash = 0
  for (const char of name) hash = (hash * 31 + char.charCodeAt(0)) | 0
  return palette[Math.abs(hash) % palette.length]
}

export function Avatar({ name, className }: { name: string; className?: string }) {
  return (
    <AvatarPrimitive.Root
      className={cn('inline-flex size-8 shrink-0 overflow-hidden rounded-full', className)}
    >
      <AvatarPrimitive.Fallback
        className={cn('flex size-full items-center justify-center text-xs font-semibold', colorFor(name))}
        aria-label={name}
      >
        {initials(name)}
      </AvatarPrimitive.Fallback>
    </AvatarPrimitive.Root>
  )
}
