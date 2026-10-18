import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { sessionStore } from '@/auth/session'
import { server } from '@/test/server'
import { renderRoutes } from '@/test/render'
import { LoginPage } from './login-page'

const routes = [
  { path: '/login', element: <LoginPage /> },
  { path: '*', element: <p>Somewhere in the app</p> },
]

describe('LoginPage', () => {
  it('validates before sending anything', async () => {
    const { user } = renderRoutes(routes, '/login')

    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument()
    expect(screen.getByText('Enter your password')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
  })

  it('shows a clear message for wrong credentials', async () => {
    server.use(
      http.post('*/api/auth/login', () =>
        HttpResponse.json({ status: 401, detail: 'Invalid email or password' }, { status: 401 }),
      ),
    )
    const { user } = renderRoutes(routes, '/login')

    await user.type(screen.getByLabelText('Email'), 'anna@example.com')
    await user.type(screen.getByLabelText('Password'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Incorrect email or password.')
    expect(sessionStore.getSnapshot()).toBeNull()
  })

  it('starts a session and goes back to the page that asked for login', async () => {
    server.use(
      http.post('*/api/auth/login', () =>
        HttpResponse.json({ accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 3600 }),
      ),
    )
    const { user, router } = renderRoutes(routes, '/login?redirect=%2Fgroups%2F42')

    await user.type(screen.getByLabelText('Email'), 'anna@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Somewhere in the app')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/groups/42')
    expect(sessionStore.token()).toBe('jwt-token')
    sessionStore.end()
  })

  it('never redirects outside the app', async () => {
    server.use(
      http.post('*/api/auth/login', () =>
        HttpResponse.json({ accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 3600 }),
      ),
    )
    const { user, router } = renderRoutes(routes, '/login?redirect=%2F%2Fevil.example.com')

    await user.type(screen.getByLabelText('Email'), 'anna@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await screen.findByText('Somewhere in the app')
    expect(router.state.location.pathname).toBe('/')
    sessionStore.end()
  })
})
