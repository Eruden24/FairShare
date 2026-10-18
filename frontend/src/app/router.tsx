import { createBrowserRouter } from 'react-router'
import { PublicOnly, RequireAuth } from '@/auth/route-guards'
import { NotFoundPage, RouteErrorPage } from './status-pages'

/**
 * Pages are loaded on demand (`lazy`), so the first download only contains the page you open.
 */
export const router = createBrowserRouter([
  {
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <PublicOnly />,
        children: [
          {
            path: '/login',
            lazy: () => import('@/features/auth/login-page').then((m) => ({ Component: m.LoginPage })),
          },
          {
            path: '/register',
            lazy: () => import('@/features/auth/register-page').then((m) => ({ Component: m.RegisterPage })),
          },
        ],
      },
      {
        element: <RequireAuth />,
        children: [
          {
            lazy: () => import('@/components/layout/app-shell').then((m) => ({ Component: m.AppShell })),
            children: [
              {
                index: true,
                lazy: () =>
                  import('@/features/dashboard/dashboard-page').then((m) => ({ Component: m.DashboardPage })),
              },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
])
