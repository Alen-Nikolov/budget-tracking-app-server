-- Creates the "budget-tracker" database if it does not exist yet. Safe to re-run.
-- Run against the default "postgres" database as a role with CREATEDB:
--
--   psql -U postgres -h localhost -d postgres -f scripts/create-database.sql
--
-- Tables are NOT created here; Flyway creates them when the server starts.
SELECT 'CREATE DATABASE "budget-tracker"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'budget-tracker')\gexec
