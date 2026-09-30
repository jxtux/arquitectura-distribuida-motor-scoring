#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
COMPOSE=(docker compose --env-file .env --env-file secrets/bootstrap.env)

ssl="$(${COMPOSE[@]} exec -T postgres psql -U postgres -d postgres -tAc 'SHOW ssl;' | xargs)"
[[ "$ssl" == "on" ]] || { echo "ERROR: PostgreSQL ssl=$ssl" >&2; exit 1; }

secure="$(${COMPOSE[@]} exec -T postgres bash -lc 'PGPASSWORD="$POSTGRES_SUPER_PASSWORD" PGSSLMODE=verify-full PGSSLROOTCERT=/var/lib/postgresql/tls/ca.crt psql -h postgres -U postgres -d postgres -tAc "SELECT ssl::text || '\''|'\'' || version || '\''|'\'' || cipher FROM pg_stat_ssl WHERE pid=pg_backend_pid();"' | xargs)"
[[ "$secure" == true\|TLSv* ]] || { echo "ERROR: sesion TLS inesperada: $secure" >&2; exit 1; }

if ${COMPOSE[@]} exec -T postgres bash -lc 'PGPASSWORD="$POSTGRES_SUPER_PASSWORD" PGSSLMODE=disable psql -h postgres -U postgres -d postgres -tAc "SELECT 1;"' >/dev/null 2>&1; then
  echo 'ERROR: PostgreSQL acepto TCP sin TLS.' >&2
  exit 1
fi

echo "OK PostgreSQL TLS: $secure; TCP sin SSL rechazado."
