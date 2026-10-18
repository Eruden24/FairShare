import { isRouteErrorResponse, Link, useRouteError } from 'react-router'
import { Button } from '@/components/ui/button'

function StatusPage({ code, title, message }: { code: string; title: string; message: string }) {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-3 px-6 text-center">
      <p className="text-sm font-semibold text-primary">{code}</p>
      <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
      <p className="max-w-md text-muted-foreground">{message}</p>
      <Button asChild className="mt-4">
        <Link to="/">Back to dashboard</Link>
      </Button>
    </div>
  )
}

export function NotFoundPage() {
  return (
    <StatusPage
      code="404"
      title="Page not found"
      message="The page you're looking for doesn't exist or was moved."
    />
  )
}

/** Shown when a page crashes while rendering, instead of a blank screen. */
export function RouteErrorPage() {
  const error = useRouteError()
  if (isRouteErrorResponse(error) && error.status === 404) return <NotFoundPage />
  console.error(error)
  return (
    <StatusPage
      code="Error"
      title="Something went wrong"
      message="An unexpected error occurred. Try again, or go back to the dashboard."
    />
  )
}
