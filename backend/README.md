# FairShare

A REST API for splitting shared expenses within groups.

**Stack:** Java 25 · Spring Boot 4 · Spring Security (JWT) · PostgreSQL 17 · Flyway · MapStruct · Testcontainers

## Running locally

Requirements: JDK 25 and Docker. Run these from the `backend/` folder:

```bash
cd backend
cp .env.example .env            # then fill in your own values
docker compose up -d            # starts PostgreSQL
./mvnw spring-boot:run          # starts the API on http://localhost:8080
```

Flyway applies the database migrations on startup.

The web frontend lives in [`frontend/`](../frontend/README.md): `cd frontend && npm install && npm run dev`,
then open http://localhost:5173.

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

After 5 failed logins for the same email within 15 minutes, logins for that email return **429** with a
`Retry-After` header (in seconds) until the 15 minutes are over, even with the right password.

There are no refresh tokens: when the token expires, log in again. Short-lived access tokens keep the API
simple and stateless; refresh tokens (stored and revocable server-side) can be added when a frontend needs
long sessions.

## Endpoints

| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/api/users` | public | Register |
| POST | `/api/auth/login` | public | Log in, returns a JWT |
| GET | `/api/users/me` | logged in | My profile |
| PATCH | `/api/users/me` | logged in | Update display name / default currency |
| PUT | `/api/users/me/password` | logged in | Change password |
| GET | `/api/users/me/balances` | logged in | My balance in each of my groups |
| GET | `/api/users/search?email=` | logged in | Find a user by exact email |
| POST | `/api/groups` | logged in | Create a group (you become its owner) |
| GET | `/api/groups` | logged in | My groups |
| GET | `/api/groups/{id}` | member | Group details |
| PATCH | `/api/groups/{id}` | owner | Rename the group |
| DELETE | `/api/groups/{id}` | owner | Delete the group (only when everyone is settled up) |
| GET | `/api/groups/{id}/members` | member | List members |
| POST | `/api/groups/{id}/members` | owner | Add a member by email |
| DELETE | `/api/groups/{id}/members/{userId}` | owner | Remove a member (only with a zero balance) |
| DELETE | `/api/groups/{id}/members/me` | member | Leave the group (only with a zero balance) |
| PUT | `/api/groups/{id}/owner` | owner | Transfer ownership to another member |
| POST | `/api/groups/{id}/expenses` | member | Add an expense |
| GET | `/api/groups/{id}/expenses` | member | List expenses, newest first (`page`, `size`, `from`, `to`, `paidById`) |
| GET | `/api/groups/{id}/expenses/{expenseId}` | member | Expense details with each participant's share |
| PUT | `/api/groups/{id}/expenses/{expenseId}` | creator or owner | Replace an expense (shares are recomputed) |
| DELETE | `/api/groups/{id}/expenses/{expenseId}` | creator or owner | Delete an expense |
| GET | `/api/groups/{id}/balances` | member | Everyone's balance in the group |
| POST | `/api/groups/{id}/settlements` | member | Record a payment between two members |
| GET | `/api/groups/{id}/settlements` | member | List settlements, newest first |
| GET | `/api/groups/{id}/settlements/suggested` | member | Who should pay whom to settle everyone up |
| DELETE | `/api/groups/{id}/settlements/{settlementId}` | recorder or owner | Delete a settlement |

## Rules

- A group is only visible to its members; for anyone else it returns **404**, so group ids can't be probed.
  A member trying an owner-only action gets **403**.
- Each group has exactly one owner (enforced by a database index). The owner must transfer ownership before leaving.
- A group's currency can't be changed after creation, because its expenses are recorded in it.
- Emails are stored in lowercase and matched case-insensitively.
- Amounts are sent and returned as decimals (`12.50`) in the group's currency, with at most as many
  decimals as the currency has (2 for EUR, 0 for JPY). They are stored as whole minor units (cents).
- The payer and every participant of an expense must be members of the group. The payer doesn't have to be a participant.
- Any member can add expenses. Only the member who added an expense, or the group owner, can edit or delete it.
- Shares always add up exactly to the expense amount, and each share is stored. When an amount doesn't divide
  exactly, the leftover cents go to randomly chosen participants (the ones who lost most to rounding first).
- A balance is `paid - owed + sent - received`: expenses someone paid, minus their shares, plus settlements they
  paid, minus settlements they received. Positive means the group owes them, negative means they owe.
  Balances in a group always add up to zero. They are computed from expenses and settlements, never stored.
- Any member can record a settlement between two members (one person often keeps the books). Settlements are
  never edited: delete a wrong one and record it again.
- Money can't disappear: nobody can leave or be removed while their balance isn't zero, and a group can only be
  deleted once everyone is settled up (**409** otherwise).
- Someone who left a group still appears in its balances (`"member": false`), with a zero balance, because their
  expenses stay in the group's history.
- Expenses carry a `version`. Send the version you last read when replacing an expense: if someone changed it in
  the meantime you get **409** instead of overwriting their change. Two edits at the very same moment are also
  caught (the second gets 409).
- The expense list is paginated (`size` up to 100, default 20). Groups and member lists are small and are not.

## Splitting an expense

`splitType` is optional and defaults to `EQUAL`. Each participant may carry a `value`, whose meaning depends on it:

| `splitType` | `value` | Rule | Example: 90.00 between Anna and Bob |
|---|---|---|---|
| `EQUAL` | leave it out | same share for everyone | 45.00 / 45.00 |
| `EXACT` | the amount | values must add up to the amount | 60.00 / 30.00 |
| `PERCENTAGE` | a percentage, up to 2 decimals | values must add up to 100 | 66.67 / 33.33 → 60.00 / 30.00 |
| `SHARES` | a number of shares, up to 2 decimals | any positive numbers | 2 / 1 → 60.00 / 30.00 |

```json
{
  "description": "Cabin",
  "amount": 90.00,
  "paidById": "…",
  "splitType": "SHARES",
  "participants": [ { "userId": "…anna", "value": 2 }, { "userId": "…bob", "value": 1 } ],
  "date": "2026-10-01"
}
```

## Settling up

`GET /api/groups/{id}/settlements/suggested` lists the transfers that bring every balance to zero. The person
who owes the most pays the person who is owed the most, and so on, so `n` people need at most `n - 1` transfers.
Record each one as it happens:

```json
POST /api/groups/{id}/settlements
{ "fromUserId": "…carol", "toUserId": "…bob", "amount": 30.00, "date": "2026-10-05" }
```

## Errors

Every error, including 401 and 403 from the security layer, uses the standard
[Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Validation failed",
  "errors": { "currency": "must be a valid currency code like EUR" }
}
```

| Status | When |
|---|---|
| 400 | Invalid body, query parameter or path variable. `errors` maps each field to its problem |
| 401 | Missing, invalid or expired token; wrong email or password |
| 403 | A member trying something only the owner (or the creator) may do |
| 404 | Not found, or a group you're not a member of |
| 409 | Conflicts: duplicate email or member, open balances, an outdated `version` |
| 429 | Too many failed logins; see `Retry-After` |

## Data model

```
users ──< user_groups >── groups
          (role: OWNER | MEMBER, joined_at)

groups ──< expenses ──< expense_splits >── users
           (amount_minor, paid_by,      (amount_minor, split_value)
            date, split_type, version)

groups ──< settlements >── users
           (from_user, to_user, amount_minor, settled_on)
```
