/**
 * Every API error is a Problem Details body (RFC 9457). Validation errors add `errors`: field -> message.
 */
export type ProblemDetail = {
  status: number
  title?: string
  detail?: string
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${problem.status}`)
    this.name = 'ApiError'
    this.status = problem.status
    this.problem = problem
  }

  /** Field errors from the backend, e.g. { email: "must be a well-formed email address" } */
  get fieldErrors(): Record<string, string> {
    return this.problem.errors ?? {}
  }
}

/** A message that's safe to show to the user for any thrown value. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof TypeError) return 'Could not reach the server. Check your connection and try again.'
  return 'Something went wrong. Please try again.'
}
