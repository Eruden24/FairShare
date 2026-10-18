import { Navigate, Outlet, useLocation } from 'react-router'
import { sessionStore } from './session'
import { useSession } from './use-session'

/**
 * Pages behind login. Without a session, go to /login, and come back here after logging in again,
 * unless the user signed out on purpose: then whoever logs in next starts from the dashboard.
 */
export function RequireAuth() {
  const session = useSession()
  const location = useLocation()

  if (!session) {
    const here = location.pathname + location.search
    const comeBack = sessionStore.endReason() !== 'signed-out' && here !== '/'
    return <Navigate to={comeBack ? `/login?redirect=${encodeURIComponent(here)}` : '/login'} replace />
  }
  return <Outlet />
}

/** Login and register: already logged in users go straight to the app. */
export function PublicOnly() {
  const session = useSession()
  return session ? <Navigate to="/" replace /> : <Outlet />
}
