import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError, errorMessage } from '@/api/errors'

/**
 * Puts a failed request's errors on the form: the backend's field errors next to their fields,
 * anything else in the form-level `root` error.
 */
export function applyServerErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: Path<T>[],
) {
  if (error instanceof ApiError) {
    let placed = false
    for (const [field, message] of Object.entries(error.fieldErrors)) {
      if ((fields as string[]).includes(field)) {
        setError(field as Path<T>, { message })
        placed = true
      }
    }
    if (placed) return
  }
  setError('root', { message: errorMessage(error) })
}
