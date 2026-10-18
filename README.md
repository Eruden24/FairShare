# FairShare

A REST API for splitting shared expenses within groups.

**Stack:** Java 25 · Spring Boot 4 · Spring Security (JWT) · PostgreSQL 17 · Flyway · MapStruct · Testcontainers

## Running locally

Requirements: JDK 25 and Docker.

```bash
cp .env.example .env            # then fill in your own values
docker compose up -d            # starts PostgreSQL
./mvnw spring-boot:run          # starts the API on http://localhost:8080
```

Flyway applies the database migrations on startup.

API docs (Swagger UI): http://localhost:8080/swagger-ui.html. To call protected endpoints from Swagger,
log in with `POST /api/auth/login`, click **Authorize**, and paste the `accessToken`.

## Tests

```bash
./mvnw test
```

The integration tests start a throwaway PostgreSQL container, so Docker must be running.

## Authentication

1. Register: `POST /api/users`
2. Log in: `POST /api/auth/login` returns `{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 3600 }`
3. Send `Authorization: Bearer <accessToken>` on every other request.

Tokens are signed with `JWT_SECRET` (HS256) and expire after `JWT_EXPIRATION` (default `PT1H`).

## Endpoints

| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/api/users` | public | Register |
| POST | `/api/auth/login` | public | Log in, returns a JWT |
| GET | `/api/users/me` | logged in | My profile |
| PATCH | `/api/users/me` | logged in | Update display name / default currency |
| PUT | `/api/users/me/password` | logged in | Change password |
| GET | `/api/users/search?email=` | logged in | Find a user by exact email |
| POST | `/api/groups` | logged in | Create a group (you become its owner) |
| GET | `/api/groups` | logged in | My groups |
| GET | `/api/groups/{id}` | member | Group details |
| PATCH | `/api/groups/{id}` | owner | Rename the group |
| DELETE | `/api/groups/{id}` | owner | Delete the group |
| GET | `/api/groups/{id}/members` | member | List members |
| POST | `/api/groups/{id}/members` | owner | Add a member by email |
| DELETE | `/api/groups/{id}/members/{userId}` | owner | Remove a member |
| DELETE | `/api/groups/{id}/members/me` | member | Leave the group |
| PUT | `/api/groups/{id}/owner` | owner | Transfer ownership to another member |
| POST | `/api/groups/{id}/expenses` | member | Add an expense, split equally between participants |
| GET | `/api/groups/{id}/expenses/{expenseId}` | member | Expense details with each participant's share |

## Rules

- A group is only visible to its members; for anyone else it returns **404**, so group ids can't be probed.
  A member trying an owner-only action gets **403**.
- Each group has exactly one owner (enforced by a database index). The owner must transfer ownership before leaving.
- A group's currency can't be changed after creation, because its expenses are recorded in it.
- Emails are stored in lowercase and matched case-insensitively.
- Amounts are sent and returned as decimals (`12.50`) in the group's currency, with at most as many
  decimals as the currency has (2 for EUR, 0 for JPY). They are stored as whole minor units (cents).
- An expense is split equally between its participants. When it doesn't divide exactly, the leftover
  cents go to randomly chosen participants, so the shares always add up to the total. Each share is stored.
- The payer and every participant of an expense must be members of the group. The payer doesn't have to be a participant.
- Lists are not paginated yet. Groups and their member lists are small; pagination will come with expenses.

## Errors

Errors use the standard [Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Validation failed",
  "errors": { "currency": "must be a valid currency code like EUR" }
}
```

## Data model

```
users ──< user_groups >── groups
          (role: OWNER | MEMBER, joined_at)

groups ──< expenses ──< expense_splits >── users
           (amount_minor, paid_by, date)   (amount_minor)
```
