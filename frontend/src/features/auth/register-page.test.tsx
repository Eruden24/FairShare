import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '@/test/server'
import { renderRoutes } from '@/test/render'
import { RegisterPage } from './register-page'

const routes = [
  { path: '/register', element: <RegisterPage /> },
  { path: '*', element: <p>Dashboard</p> },
]

async function fillIn(user: ReturnType<typeof renderRoutes>['user'], confirm = 'password123') {
  await user.type(screen.getByLabelText('Name'), 'Anna Smith')
  await user.type(screen.getByLabelText('Email'), 'anna@example.com')
  await user.type(screen.getByLabelText('Password'), 'password123')
  await user.type(screen.getByLabelText('Confirm password'), confirm)
}

describe('RegisterPage', () => {
  it('checks that the passwords match', async () => {
    const { user } = renderRoutes(routes, '/register')

    await fillIn(user, 'something-else')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Passwords do not match')).toBeInTheDocument()
  })

  it('puts the backend error next to the field it belongs to', async () => {
    server.use(
      http.post('*/api/users', () =>
        HttpResponse.json(
          { status: 400, detail: 'Validation failed', errors: { email: 'is already used by someone else' } },
          { status: 400 },
        ),
      ),
    )
    const { user } = renderRoutes(routes, '/register')

    await fillIn(user)
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('is already used by someone else')).toBeInTheDocument()
  })

  it('registers, logs in and opens the app', async () => {
    let registered: unknown
    server.use(
      http.post('*/api/users', async ({ request }) => {
        registered = await request.json()
        return HttpResponse.json({}, { status: 201 })
      }),
      http.post('*/api/auth/login', () =>
        HttpResponse.json({ accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 3600 }),
      ),
    )
    const { user } = renderRoutes(routes, '/register')

    await fillIn(user)
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Dashboard')).toBeInTheDocument()
    // confirmPassword stays in the browser
    expect(registered).toEqual({
      displayName: 'Anna Smith',
      email: 'anna@example.com',
      password: 'password123',
      defaultCurrency: 'EUR',
    })
  })
})
