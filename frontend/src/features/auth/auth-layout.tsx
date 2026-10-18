import type { ReactNode } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { Logo } from '@/components/layout/logo'

const highlights = [
  'Split by equal parts, exact amounts, percentages or shares',
  'Balances always add up to the cent',
  'Settle up with the fewest possible transfers',
]

/** Two columns on large screens: the product pitch on the left, the form on the right. */
export function AuthLayout({
  title,
  subtitle,
  children,
}: {
  title: string
  subtitle: ReactNode
  children: ReactNode
}) {
  return (
    <div className="grid min-h-screen lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
      <aside className="relative hidden overflow-hidden bg-gradient-to-br from-indigo-600 via-indigo-700 to-violet-800 p-12 text-white lg:flex lg:flex-col">
        <Logo inverted className="text-white" />
        <div className="mt-auto max-w-md">
          <h2 className="text-3xl font-semibold tracking-tight">
            Shared expenses, without the awkward maths.
          </h2>
          <ul className="mt-8 grid gap-4 text-indigo-100">
            {highlights.map((text) => (
              <li key={text} className="flex items-start gap-3">
                <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-indigo-300" aria-hidden />
                {text}
              </li>
            ))}
          </ul>
        </div>
        <p className="mt-12 text-xs text-indigo-200">© {new Date().getFullYear()} FairShare</p>
        <div
          className="pointer-events-none absolute -top-32 -right-32 size-96 rounded-full bg-white/5"
          aria-hidden
        />
      </aside>

      <main className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <Logo className="mb-10 lg:hidden" />
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          <p className="mt-2 text-muted-foreground">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </main>
    </div>
  )
}
