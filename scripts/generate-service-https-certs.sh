#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/platform/certs"
if [[ -z "${DEV_CERT_PASSWORD:-}" && -f "$ROOT/secrets/bootstrap.env" ]]; then
  set -a
  source "$ROOT/secrets/bootstrap.env"
  set +a
fi
PASS="${DEV_CERT_PASSWORD:?DEV_CERT_PASSWORD missing}"
[[ -f "$OUT/ca.crt" && -f "$OUT/ca.key" ]] || { echo "Missing ca.crt/ca.key" >&2; exit 2; }

services=(iam-service credit-service payment-service scoring-service report-service notification-service audit-service query-service)
for svc in "${services[@]}"; do
  base="$OUT/${svc}-https"
  if [[ -f "${base}.p12" && "${FORCE:-0}" != "1" ]]; then
    echo "Keeping existing ${svc}-https.p12"
    continue
  fi
  openssl genrsa -out "${base}.key" 2048
  openssl req -new -key "${base}.key" -subj "/CN=${svc}" -out "${base}.csr"
  cat > "${base}.ext" <<EXT
subjectAltName=DNS:${svc},DNS:localhost,IP:127.0.0.1
extendedKeyUsage=serverAuth
keyUsage=digitalSignature,keyEncipherment
EXT
  openssl x509 -req -in "${base}.csr" -CA "$OUT/ca.crt" -CAkey "$OUT/ca.key" -CAcreateserial \
    -out "${base}.crt" -days 825 -sha256 -extfile "${base}.ext"
  openssl pkcs12 -export -name "$svc" -in "${base}.crt" -inkey "${base}.key" -certfile "$OUT/ca.crt" \
    -out "${base}.p12" -password "pass:$PASS"
  rm -f "${base}.csr" "${base}.ext"
  chmod 600 "${base}.key" "${base}.p12" || true
  chmod 644 "${base}.crt" || true
  echo "HTTPS certificate ready: $svc"
done
rm -f "$OUT"/*.srl
echo "Internal microservice HTTPS certificates ready."
