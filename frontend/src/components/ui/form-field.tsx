import { useId, type ReactElement } from 'react'
import { cloneElement } from 'react'
import { Label } from './label'

type FormFieldProps = {
  label: string
  /** The validation message to show, if any */
  error?: string
  hint?: string
  /** One input-like element; it gets the id and the aria attributes that tie it to the label and the error */
  children: ReactElement<{ id?: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }>
}

/** Label + control + hint or error, wired up for screen readers. */
export function FormField({ label, error, hint, children }: FormFieldProps) {
  const id = useId()
  const messageId = `${id}-message`
  const message = error ?? hint

  return (
    <div className="grid gap-1.5">
      <Label htmlFor={id}>{label}</Label>
      {cloneElement(children, {
        id,
        'aria-invalid': error ? true : undefined,
        'aria-describedby': message ? messageId : undefined,
      })}
      {message && (
        <p id={messageId} className={error ? 'text-xs text-destructive' : 'text-xs text-muted-foreground'}>
          {message}
        </p>
      )}
    </div>
  )
}
