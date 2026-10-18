import { LayoutDashboard, type LucideIcon } from 'lucide-react'
import { NavLink, Outlet } from 'react-router'
import { cn } from '@/lib/utils'
import { Logo } from './logo'
import { UserMenu } from './user-menu'

type NavItem = { to: string; label: string; icon: LucideIcon; end?: boolean }

// New sections (groups, settings) are added here as they're built
const navigation: NavItem[] = [{ to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true }]

/** The frame around every logged-in page: sidebar on large screens, top bar on small ones. */
export function AppShell() {
  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[16rem_minmax(0,1fr)]">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded-md focus:bg-card focus:px-3 focus:py-2 focus:shadow"
      >
        Skip to content
      </a>

      <aside className="hidden border-r bg-sidebar text-sidebar-foreground lg:sticky lg:top-0 lg:flex lg:h-screen lg:flex-col">
        <div className="flex h-16 items-center px-5">
          <Logo />
        </div>
        <nav aria-label="Main" className="flex-1 px-3 py-2">
          <ul className="grid gap-0.5">
            {navigation.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-md px-3 py-2 font-medium transition-colors',
                      isActive ? 'bg-accent text-accent-foreground' : 'hover:bg-muted hover:text-foreground',
                    )
                  }
                >
                  <item.icon className="size-4" aria-hidden />
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="border-t p-3">
          <UserMenu />
        </div>
      </aside>

      <div className="flex min-w-0 flex-col">
        <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b bg-card/80 px-4 backdrop-blur lg:hidden">
          <Logo />
          <div className="w-48">
            <UserMenu />
          </div>
        </header>
        <main id="main" className="flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
          <div className="mx-auto max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
