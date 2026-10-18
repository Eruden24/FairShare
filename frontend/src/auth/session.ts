/**
 * The logged-in session: the JWT and when it expires. Kept in localStorage so a reload or a new tab stays
 * logged in, and exposed as a tiny store (subscribe + getSnapshot) for React's useSyncExternalStore.
 *
 * Trade-off: localStorage is readable by any script on the page, so an XSS bug could steal the token.
 * The safer setup is an httpOnly cookie set by the backend; the API uses Bearer tokens today, and this
 * module is the only place that would change.
 */
export type Session = { token: string; expiresAt: number }
/** Why there's no session: never logged in, the token expired or was rejected, or the user signed out */
export type EndReason = 'none' | 'expired' | 'signed-out'

const STORAGE_KEY = 'fairshare.session'
const listeners = new Set<() => void>()

function read(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as Session
    return session.expiresAt > Date.now() ? session : null
  } catch {
    return null
  }
}

let current: Session | null = read()
let endReason: EndReason = 'none'

function emit() {
  listeners.forEach((listener) => listener())
}

export const sessionStore = {
  getSnapshot: () => current,
  subscribe(listener: () => void) {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
  endReason: () => endReason,
  start(token: string, expiresInSeconds: number) {
    current = { token, expiresAt: Date.now() + expiresInSeconds * 1000 }
    endReason = 'none'
    localStorage.setItem(STORAGE_KEY, JSON.stringify(current))
    emit()
  },
  end(reason: Exclude<EndReason, 'none'> = 'expired') {
    current = null
    endReason = reason
    localStorage.removeItem(STORAGE_KEY)
    emit()
  },
  /** The token if it's still valid; ends an expired session on the way. */
  token() {
    if (current && current.expiresAt <= Date.now()) sessionStore.end()
    return current?.token ?? null
  },
}

// Logging in or out in another tab updates this one too
window.addEventListener('storage', (event) => {
  if (event.key === STORAGE_KEY) {
    current = read()
    emit()
  }
})
