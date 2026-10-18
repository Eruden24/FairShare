import { useSyncExternalStore } from 'react'
import { sessionStore } from './session'

/** The current session, re-rendering when the user logs in or out (in this tab or another one). */
export function useSession() {
  return useSyncExternalStore(sessionStore.subscribe, sessionStore.getSnapshot)
}
