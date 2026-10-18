import createClient, { type Middleware } from 'openapi-fetch'
import { sessionStore } from '@/auth/session'
import { ApiError, type ProblemDetail } from './errors'
import type { paths } from './schema'

/**
 * The typed API client. Paths, parameters and bodies are checked against the backend's OpenAPI spec:
 * api.GET('/api/groups/{groupId}', { params: { path: { groupId } } }) only compiles if that endpoint exists.
 * Regenerate the types after a backend change: npm run api:fetch && npm run api:generate
 */
// Same origin as the page: in development Vite forwards /api to Spring.
// fetch is looked up on every call (not captured once), so tools that replace it, like MSW in tests, work.
export const api = createClient<paths>({
  baseUrl: window.location.origin,
  fetch: (request) => globalThis.fetch(request),
})

const auth: Middleware = {
  onRequest({ request }) {
    const token = sessionStore.token()
    if (token) request.headers.set('Authorization', `Bearer ${token}`)
    return request
  },
  onResponse({ request, response }) {
    // The token expired or was rejected: end the session, the router then sends the user to the login page.
    // Not for the login call itself, where 401 just means a wrong password.
    if (response.status === 401 && !new URL(request.url).pathname.endsWith('/auth/login')) {
      sessionStore.end()
    }
    return response
  },
}

api.use(auth)

/**
 * Turns openapi-fetch's { data, error, response } into "return data or throw ApiError",
 * which is what TanStack Query and our forms expect.
 */
export async function unwrap<T>(
  call: Promise<{ data?: T; error?: unknown; response: Response }>,
): Promise<T> {
  const { data, error, response } = await call
  if (!response.ok) {
    const problem = (error as ProblemDetail | undefined) ?? { status: response.status }
    throw new ApiError({ ...problem, status: problem.status ?? response.status })
  }
  return data as T
}
