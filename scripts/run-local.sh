#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

[[ -f .env ]] || cp .env.example .env
[[ -f secrets/bootstrap.env ]] || ./scripts/prepare-local-secrets.sh

required_certs=(
  platform/certs/client.truststore.jks
  platform/certs/kong-server.crt
  platform/certs/iam-grpc-server.crt
  platform/certs/notification-grpc-client.crt
  platform/certs/postgres-server.crt
  platform/certs/postgres-server.key
  platform/certs/ca.crt
  platform/certs/iam-public.pem
)
for f in "${required_certs[@]}"; do
  if [[ ! -s "$f" ]]; then
    ./scripts/generate-dev-certs.sh
    break
  fi
done

service_keystores=(iam-service credit-service payment-service scoring-service report-service notification-service audit-service query-service)
for svc in "${service_keystores[@]}"; do
  if [[ ! -s "platform/certs/${svc}-https.p12" ]]; then
    ./scripts/generate-service-https-certs.sh
    break
  fi
done

# Render declarative Kong config with JWT public key and internal CA.
if command -v pwsh >/dev/null 2>&1; then
  pwsh -NoProfile -File ./scripts/render-kong-config.ps1
else
  python3 ./scripts/render-kong-config.py
fi

docker compose --env-file .env --env-file secrets/bootstrap.env config >/dev/null
docker compose --env-file .env --env-file secrets/bootstrap.env up --build -d

if [[ -x ./scripts/verify-postgres-tls.sh ]]; then
  ./scripts/verify-postgres-tls.sh
fi
