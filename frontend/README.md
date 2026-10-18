# FairShare web

The React frontend for the FairShare API.

**Stack:** React 19 · TypeScript · Vite · TanStack Query · React Router · Tailwind CSS 4 · Radix UI ·
react-hook-form + zod · openapi-fetch · Vitest + Testing Library + MSW

## Running locally

Requirements: Node 20.19+ and the backend running on http://localhost:8080 (see [`backend/README.md`](../backend/README.md)).

```bash
npm install
npm run dev          # http://localhost:5173
```

Vite forwards every `/api/...` request to the backend, so the browser only ever talks to one origin and the
backend needs no CORS setup. To use a backend on another port: `API_URL=http://localhost:8081 npm run dev`.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Development server with hot reload |
| `npm run build` | Type-check and build for production into `dist/` |
| `npm test` | Run the tests once (`npm run test:watch` to keep them running) |
| `npm run lint` / `npm run format` | ESLint / Prettier |
| `npm run api:fetch` | Download the OpenAPI spec from the running backend into `openapi.json` |
| `npm run api:generate` | Regenerate the TypeScript types in `src/api/schema.d.ts` from `openapi.json` |

## The API contract

The types in `src/api/schema.d.ts` are **generated** from the backend's OpenAPI spec, never written by hand.
After changing an endpoint or a DTO in the backend:

```bash
npm run api:fetch && npm run api:generate && npm run typecheck
```

Anything in the frontend that no longer matches the backend then fails to compile.

## Structure

```
src/
  app/          App setup: providers, router, query client, error pages
  api/          Typed client, generated schema, error type, query keys
  auth/         Session (token) store, route guards
  components/
    ui/         Design-system components (Button, Input, Card, ...), built on Radix UI
    layout/     App shell: sidebar, top bar, user menu, page header
  features/     One folder per feature: its pages, API hooks, form schemas and tests
  lib/          Small helpers: formatting, currencies, class names
  test/         Test setup: MSW server, render helpers
```

Conventions:

- **Server data lives in TanStack Query**, not in component state. Every query key is in `api/query-keys.ts`.
- **Forms** use react-hook-form with a zod schema that mirrors the backend's validation; server-side field
  errors are shown next to their field (`lib/form-errors.ts`).
- **Colors come from design tokens** (`bg-primary`, `text-muted-foreground`, ...) defined in `src/index.css`,
  never raw values, so the theme (and dark mode) can change in one place.
- **Pages are loaded lazily**, and libraries are split into long-cached vendor chunks.
- **Tests run against MSW**, which answers `fetch` calls like the real API, so they exercise the real client code.

## Authentication

The JWT from `POST /api/auth/login` is kept in `localStorage` with its expiry (`auth/session.ts`). Every request
sends it as a Bearer token; a 401 ends the session and the route guard shows the login page. If the session
expired, the user comes back to the page they were on after logging in again; after a deliberate sign-out they
start from the dashboard.
