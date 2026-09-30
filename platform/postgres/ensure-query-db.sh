#!/usr/bin/env bash
set -euo pipefail
export PGPASSWORD="${POSTGRES_SUPER_PASSWORD:?POSTGRES_SUPER_PASSWORD required}"
export PGSSLMODE="${PGSSLMODE:-verify-full}"
export PGSSLROOTCERT="${PGSSLROOTCERT:-/run/secrets/ca.crt}"
: "${QUERY_DB_PASSWORD:?QUERY_DB_PASSWORD required}"
HOST="${POSTGRES_HOST:-postgres}"; PORT="${POSTGRES_PORT:-5432}"
until psql -h "$HOST" -p "$PORT" -U postgres -d postgres -tAc 'SELECT 1' >/dev/null 2>&1; do sleep 2; done
psql -h "$HOST" -p "$PORT" -U postgres -d postgres -v ON_ERROR_STOP=1 \
  --set=pass="$QUERY_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE query_user LOGIN PASSWORD %L', :'pass')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='query_user') \gexec
SELECT format('ALTER ROLE query_user WITH LOGIN PASSWORD %L', :'pass') \gexec
SELECT 'CREATE DATABASE query_db OWNER query_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname='query_db') \gexec
ALTER DATABASE query_db OWNER TO query_user;
SQL
echo 'query_db ready.'
