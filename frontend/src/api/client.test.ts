import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { sessionStore } from '@/auth/session'
import { server } from '@/test/server'
import { api, unwrap } from './client'
import { ApiError } from './errors'

describe('api client', () => {
  it('sends the token and returns the data', async () => {
    let authorization: string | null = null
    server.use(
      http.get('*/api/users/me', ({ request }) => {
        authorization = request.headers.get('Authorization')
        return HttpResponse.json({
          id: '1',
          email: 'a@b.c',
          displayName: 'A',
          defaultCurrency: 'EUR',
          createdAt: '',
        })
      }),
    )
    sessionStore.start('abc', 3600)

    const me = await unwrap(api.GET('/api/users/me'))

    expect(me.displayName).toBe('A')
    expect(authorization).toBe('Bearer abc')
    sessionStore.end()
  })

  it('throws an ApiError with the field errors from Problem Details', async () => {
    server.use(
      http.post('*/api/groups', () =>
        HttpResponse.json(
          {
            status: 400,
            detail: 'Validation failed',
            errors: { currency: 'must be a valid currency code like EUR' },
          },
          { status: 400 },
        ),
      ),
    )

    const error = await unwrap(api.POST('/api/groups', { body: { name: 'Trip', currency: 'XYZ' } })).catch(
      (e) => e,
    )

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.fieldErrors).toEqual({ currency: 'must be a valid currency code like EUR' })
  })

  it('ends the session when the token is rejected', async () => {
    server.use(http.get('*/api/groups', () => HttpResponse.json({ status: 401 }, { status: 401 })))
    sessionStore.start('expired', 3600)

    await unwrap(api.GET('/api/groups')).catch(() => undefined)

    expect(sessionStore.getSnapshot()).toBeNull()
  })
})
