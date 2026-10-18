import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, unwrap } from '@/api/client'
import { queryKeys } from '@/api/query-keys'
import type { CreateUserRequest, LoginRequest } from '@/api/types'
import { sessionStore } from '@/auth/session'
import { useSession } from '@/auth/use-session'

/** The logged-in user's profile. Only runs while there's a session. */
export function useMe() {
  const session = useSession()
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: () => unwrap(api.GET('/api/users/me')),
    enabled: session !== null,
    staleTime: 5 * 60 * 1000,
  })
}

export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: LoginRequest) => unwrap(api.POST('/api/auth/login', { body })),
    onSuccess: (token) => {
      // Nothing cached from a previous user may leak into this session
      queryClient.clear()
      sessionStore.start(token.accessToken, token.expiresIn)
    },
  })
}

/** Registers, then logs straight in with the same credentials. */
export function useRegister() {
  const login = useLogin()
  return useMutation({
    mutationFn: async (body: CreateUserRequest) => {
      await unwrap(api.POST('/api/users', { body }))
      return login.mutateAsync({ email: body.email, password: body.password })
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  // The route guard sees the session end and shows the login page
  return () => {
    sessionStore.end('signed-out')
    queryClient.clear()
  }
}
