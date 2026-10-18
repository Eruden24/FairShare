import { ChevronsUpDown, LogOut } from 'lucide-react'
import { Avatar } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Skeleton } from '@/components/ui/skeleton'
import { useLogout, useMe } from '@/features/auth/api'

export function UserMenu() {
  const { data: me } = useMe()
  const logout = useLogout()

  if (!me) return <Skeleton className="h-11 w-full" />

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex w-full items-center gap-3 rounded-lg p-2 text-left hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none">
        <Avatar name={me.displayName} />
        <span className="min-w-0 flex-1">
          <span className="block truncate font-medium text-foreground">{me.displayName}</span>
          <span className="block truncate text-xs text-muted-foreground">{me.email}</span>
        </span>
        <ChevronsUpDown className="size-4 text-muted-foreground" aria-hidden />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" side="top" className="w-60">
        <DropdownMenuLabel className="text-xs text-muted-foreground">
          Signed in as {me.email}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={logout}>
          <LogOut aria-hidden />
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
