import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderRoutes } from '@/test/render'
import { sessionStore } from './session'
import { RequireAuth } from './route-guards'

const routes = [
  { path: '/login', element: <p>Login page</p> },
  { element: <RequireAuth />, children: [{ path: '*', element: <p>Private page</p> }] },
]

describe('RequireAuth', () => {
  it('sends logged-out users to login and remembers where they were going', async () => {
    const { router } = renderRoutes(routes, '/groups/42')

    await screen.findByText('Login page')
    expect(router.state.location.search).toBe('?redirect=%2Fgroups%2F42')
  })

  it('shows the page when logged in', async () => {
    sessionStore.start('token', 3600)
    renderRoutes(routes, '/groups/42')

    expect(await screen.findByText('Private page')).toBeInTheDocument()
    sessionStore.end()
  })

  it('does not remember the page after signing out on purpose', async () => {
    sessionStore.start('token', 3600)
    const { router } = renderRoutes(routes, '/groups/42')
    await screen.findByText('Private page')

    sessionStore.end('signed-out')

    await screen.findByText('Login page')
    expect(router.state.location.search).toBe('')
  })
})
