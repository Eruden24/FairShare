import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router'
import { createQueryClient } from '@/app/query-client'

/**
 * Renders routes the way the app does (router + query cache), starting at `path`.
 * Returns the router so tests can check where the user ended up.
 */
export function renderRoutes(routes: RouteObject[], path = '/') {
  const queryClient = createQueryClient()
  queryClient.setDefaultOptions({ queries: { retry: false } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })

  return {
    user: userEvent.setup(),
    router,
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    ),
  }
}

export function renderPage(element: ReactElement, path = '/') {
  return renderRoutes([{ path: '*', element }], path)
}
