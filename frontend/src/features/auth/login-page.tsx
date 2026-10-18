import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { ApiError } from '@/api/errors'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { applyServerErrors } from '@/lib/form-errors'
import { useLogin } from './api'
import { AuthLayout } from './auth-layout'
import { loginSchema, type LoginValues } from './schemas'

export function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const login = useLogin()

  const form = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })
  const { errors } = form.formState

  const onSubmit = form.handleSubmit((values) =>
    login.mutate(values, {
      // Only go back to pages inside the app, never to an outside URL passed in ?redirect=
      onSuccess: () => navigate(safeRedirect(searchParams.get('redirect')), { replace: true }),
      onError: (error) => {
        if (error instanceof ApiError && error.status === 429) {
          form.setError('root', { message: 'Too many failed attempts. Wait a few minutes and try again.' })
        } else if (error instanceof ApiError && error.status === 401) {
          form.setError('root', { message: 'Incorrect email or password.' })
        } else {
          applyServerErrors(error, form.setError, ['email', 'password'])
        }
      },
    }),
  )

  return (
    <AuthLayout
      title="Sign in"
      subtitle={
        <>
          New to FairShare?{' '}
          <Link to="/register" className="font-medium text-primary hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} noValidate className="grid gap-5">
        {errors.root && <Alert variant="error">{errors.root.message}</Alert>}

        <FormField label="Email" error={errors.email?.message}>
          <Input type="email" autoComplete="email" autoFocus {...form.register('email')} />
        </FormField>

        <FormField label="Password" error={errors.password?.message}>
          <Input type="password" autoComplete="current-password" {...form.register('password')} />
        </FormField>

        <Button type="submit" size="lg" loading={login.isPending}>
          Sign in
        </Button>
      </form>
    </AuthLayout>
  )
}

function safeRedirect(target: string | null) {
  return target && target.startsWith('/') && !target.startsWith('//') ? target : '/'
}
