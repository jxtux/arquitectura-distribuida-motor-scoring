#!/usr/bin/env bash
set -euo pipefail
: "${POSTGRES_SUPER_PASSWORD:?}"
create_user_db() {
  local user="$1" db="$2" pass="$3"
  psql -v ON_ERROR_STOP=1 --username postgres --dbname postgres \
    --set=user="$user" --set=db="$db" --set=pass="$pass" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'user', :'pass')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'user') \gexec
SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'user', :'pass') \gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'db', :'user')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db') \gexec
SELECT format('ALTER DATABASE %I OWNER TO %I', :'db', :'user') \gexec
SQL
}
create_user_db iam_user iam_db "$IAM_DB_PASSWORD"
create_user_db credit_user credit_db "$CREDIT_DB_PASSWORD"
create_user_db payment_user payment_db "$PAYMENT_DB_PASSWORD"
create_user_db scoring_user scoring_db "$SCORING_DB_PASSWORD"
create_user_db report_user report_db "$REPORT_DB_PASSWORD"
create_user_db notification_user notification_db "$NOTIFICATION_DB_PASSWORD"
create_user_db audit_user audit_db "$AUDIT_DB_PASSWORD"
create_user_db query_user query_db "$QUERY_DB_PASSWORD"
