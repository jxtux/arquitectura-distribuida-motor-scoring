#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
[[ -f secrets/bootstrap.env ]] || { echo 'Falta secrets/bootstrap.env'; exit 1; }
set -a; source secrets/bootstrap.env; set +a
COMPOSE=(docker compose --env-file .env --env-file secrets/bootstrap.env)

echo '== Contextos KV =='
for svc in iam-service credit-service payment-service scoring-service report-service notification-service audit-service query-service; do
  "${COMPOSE[@]}" exec -T -e VAULT_TOKEN="$VAULT_DEV_ROOT_TOKEN_ID" vault \
    vault kv get -format=json "secret/finanscore/$svc" >/dev/null
  echo "OK: $svc"
done

echo '== AppRole habilitado =='
"${COMPOSE[@]}" exec -T -e VAULT_TOKEN="$VAULT_DEV_ROOT_TOKEN_ID" vault vault auth list | grep -q 'approle/'
echo 'OK: AppRole'

echo '== Root token NO presente en microservicios =='
for svc in iam-service credit-service payment-service scoring-service report-service notification-service audit-service query-service; do
  if "${COMPOSE[@]}" exec -T "$svc" sh -lc 'env | grep -q "^VAULT_DEV_ROOT_TOKEN_ID="'; then
    echo "ERROR: $svc recibio root token" >&2; exit 1
  fi
  echo "OK: $svc sin root token"
done
