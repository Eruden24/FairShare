import { cva, type VariantProps } from 'class-variance-authority'
import { AlertCircle, Info } from 'lucide-react'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

const alertVariants = cva(
  'flex gap-3 rounded-lg border px-4 py-3 text-sm [&>svg]:mt-0.5 [&>svg]:size-4 [&>svg]:shrink-0',
  {
    variants: {
      variant: {
        info: 'border-border bg-muted/50 text-foreground',
        error: 'border-destructive/30 bg-destructive/5 text-destructive',
      },
    },
    defaultVariants: { variant: 'info' },
  },
)

export function Alert({
  className,
  variant,
  children,
  ...props
}: ComponentProps<'div'> & VariantProps<typeof alertVariants>) {
  const Icon = variant === 'error' ? AlertCircle : Info
  return (
    <div
      role={variant === 'error' ? 'alert' : 'status'}
      className={cn(alertVariants({ variant }), className)}
      {...props}
    >
      <Icon aria-hidden />
      <div className="grid gap-0.5">{children}</div>
    </div>
  )
}
