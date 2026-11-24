# budget-tracking-app-server

Spring Boot 4 + PostgreSQL backend for the budget tracker (Android app + Next.js admin panel).

## Run locally

1. Create the database (once; safe to re-run, also usable on a hosted server):
   `psql -U postgres -h localhost -d postgres -f scripts/create-database.sql`
2. Set env vars (PowerShell): `$env:DB_PASSWORD = "<your postgres password>"`
   Optional: `DB_USERNAME` (default `postgres`), `DB_URL`, `JWT_SECRET` (>= 32 chars; **required in production**).
   For the admin panel set `CORS_ALLOWED_ORIGINS` (default `http://localhost:3000`).
3. `mvn spring-boot:run` — Flyway creates the tables on first start.

## API

All endpoints except `/login` and `/register` need `Authorization: Bearer <token>`.
The user is taken from the token, never from the request body.

| Method | Path | Body / params |
|---|---|---|
| POST | `/register` | `email, password, confirmPassword` → `{token, userId, email}` |
| POST | `/login` | `email, password` → `{token, userId, email}` |
| GET | `/category` | list my categories |
| POST | `/category/create` | `name, colour` (`#RRGGBB`) |
| PUT | `/category/edit` | `id, name, colour` |
| DELETE | `/category/delete/{id}` | 409 if the category still has expenses |
| POST | `/expenses/create` | `amount, categoryId, note?, date?` (date defaults to today) |
| PUT | `/expenses/edit` | `id, amount, categoryId, note?, date` |
| DELETE | `/expenses/delete/{id}` | |
| GET | `/expenses` | `from, to` (yyyy-MM-dd), `categoryIds` (optional, repeatable) → total, per-category totals, expense list |
| GET | `/admin/stats` | ADMIN only → `{totalUsers, totalExpenses, totalAmount}` |

The admin's own data uses the normal endpoints (the admin is also a user). Promote a user:
`psql -U postgres -d budget-tracker -v email=you@example.com -f scripts/make-admin.sql`, then log in again.

Production `JWT_SECRET`: generate it yourself (e.g. `openssl rand -base64 48`) and store it as a secret/env var on the host.

## Sessions
- Access token (JWT): 1 hour. Refresh token: 90 days, single use, replaced on every `POST /refresh` (sliding window).
- `POST /refresh {refreshToken}` → new `accessToken` + new `refreshToken`. `POST /logout {refreshToken}` revokes it server-side.
- `/login`: 5 failed attempts per email and 20 per IP per hour → 429 with `Retry-After`. Tune under `app.login-rate-limit`.
