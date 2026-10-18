import { Users } from 'lucide-react'
import { PageHeader } from '@/components/layout/page-header'
import { Card, CardContent } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useMe } from '@/features/auth/api'

function greeting(hour: number) {
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
}

export function DashboardPage() {
  const { data: me } = useMe()
  const firstName = me?.displayName.split(' ')[0]

  return (
    <>
      <PageHeader
        title={
          firstName ? `${greeting(new Date().getHours())}, ${firstName}` : <Skeleton className="h-8 w-64" />
        }
        description="Here's where you stand across your groups."
      />
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-16 text-center">
          <span className="flex size-12 items-center justify-center rounded-full bg-accent text-accent-foreground">
            <Users className="size-6" aria-hidden />
          </span>
          <h2 className="text-base font-semibold">Your groups will appear here</h2>
          <p className="max-w-sm text-muted-foreground">
            Create a group for a trip, a shared flat or a dinner, then add expenses and see who owes what.
          </p>
        </CardContent>
      </Card>
    </>
  )
}
