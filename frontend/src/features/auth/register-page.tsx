import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { FormField } from '@/components/ui/form-field'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { CURRENCIES, currencyLabel } from '@/lib/currencies'
import { applyServerErrors } from '@/lib/form-errors'
import { useRegister } from './api'
import { AuthLayout } from './auth-layout'
import { registerSchema, type RegisterValues } from './schemas'

export function RegisterPage() {
  const navigate = useNavigate()
  const register = useRegister()

  const form = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { displayName: '', email: '', password: '', confirmPassword: '', defaultCurrency: 'EUR' },
  })
  const { errors } = form.formState

  const onSubmit = form.handleSubmit(({ confirmPassword: _, ...values }) =>
    register.mutate(values, {
      onSuccess: () => navigate('/', { replace: true }),
      onError: (error) =>
        applyServerErrors(error, form.setError, ['displayName', 'email', 'password', 'defaultCurrency']),
    }),
  )

  return (
    <AuthLayout
      title="Create your account"
      subtitle={
        <>
          Already have one?{' '}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={onSubmit} noValidate className="grid gap-5">
        {errors.root && <Alert variant="error">{errors.root.message}</Alert>}

        <FormField label="Name" error={errors.displayName?.message} hint="Shown to the people in your groups">
          <Input autoComplete="name" autoFocus {...form.register('displayName')} />
        </FormField>

        <FormField label="Email" error={errors.email?.message}>
          <Input type="email" autoComplete="email" {...form.register('email')} />
        </FormField>

        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="Password" error={errors.password?.message}>
            <Input type="password" autoComplete="new-password" {...form.register('password')} />
          </FormField>
          <FormField label="Confirm password" error={errors.confirmPassword?.message}>
            <Input type="password" autoComplete="new-password" {...form.register('confirmPassword')} />
          </FormField>
        </div>

        <FormField label="Default currency" error={errors.defaultCurrency?.message}>
          <NativeSelect {...form.register('defaultCurrency')}>
            {CURRENCIES.map((code) => (
              <option key={code} value={code}>
                {currencyLabel(code)}
              </option>
            ))}
          </NativeSelect>
        </FormField>

        <Button type="submit" size="lg" loading={register.isPending}>
          Create account
        </Button>
      </form>
    </AuthLayout>
  )
}
