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

## Email verification
- `/register` sends a link (`GET /verify-email?token=...`, valid 24 h); `/login` refuses unverified accounts. See `docs/API.md`.
- **No mail server configured (default for local dev):** the link is printed in the server console (`Verification link for ...`).
- **Real emails:** set `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` (usually 587), `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`,
  `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`, and `MAIL_FROM` (any SMTP provider works: Brevo, Mailgun, Resend, SendGrid, Gmail app password...).
- `PUBLIC_BASE_URL`: address of this server as seen from the user's phone/browser (NOT localhost for a real phone).
- Redirect targets after clicking: `VERIFY_WEB_REDIRECT_URL`, `VERIFY_ANDROID_REDIRECT_URL`.

## Deploying
Everything is configured through environment variables; nothing in the code refers to localhost except the defaults.

| Variable | Purpose |
|---|---|
| `PORT` | port to listen on (hosts set this; default 8080) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `jdbc:postgresql://HOST:5432/DBNAME` (+ `?sslmode=require` if needed) |
| `JWT_SECRET` | long random secret (`openssl rand -base64 48`) |
| `CORS_ALLOWED_ORIGINS` | admin panel origin, e.g. `https://admin.yourapp.com` (no trailing slash) |
| `PUBLIC_BASE_URL` | this server's public https address (used in the verification email link) |
| `VERIFY_WEB_REDIRECT_URL`, `VERIFY_ANDROID_REDIRECT_URL` | where the email link sends the user afterwards |
| `SPRING_MAIL_*`, `MAIL_FROM` | SMTP settings; send from your own domain (SPF/DKIM) |
| `FORWARD_HEADERS_STRATEGY` | set to `framework` when behind the host's proxy/load balancer, so the login rate limit sees real client IPs. Leave unset locally. |

- Health check for the host: `GET /actuator/health` (public, no details, reports DB status).
- Build a jar: `./mvnw package` then `java -jar target/budget-tracker-server-*.jar`. Or build the `Dockerfile` (Java 27, runs as non-root).
- Tables are created by Flyway on first start. Create an admin afterwards with `scripts/make-admin.sql`.
- The login rate limiter is in memory: run a single instance.
