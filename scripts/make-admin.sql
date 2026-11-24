-- Promotes an existing (registered) user to ADMIN. Safe to re-run.
--
--   psql -U postgres -h localhost -d "budget-tracker" -v email=you@example.com -f scripts/make-admin.sql
--
-- The user must log in again afterwards to get a token that carries the ADMIN role.
UPDATE users SET role = 'ADMIN' WHERE email = lower(:'email');
