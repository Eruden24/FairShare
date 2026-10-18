import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '@/api/errors'

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30 * 1000,
        // Retrying a 4xx can't help (not found, forbidden...); retry network and server errors twice
        retry: (failureCount, error) =>
          !(error instanceof ApiError && error.status >= 400 && error.status < 500) && failureCount < 2,
      },
    },
  })
}
