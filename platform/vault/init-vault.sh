#!/usr/bin/env sh
set -eu
export VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
export VAULT_TOKEN="${VAULT_DEV_ROOT_TOKEN_ID:?VAULT_DEV_ROOT_TOKEN_ID is required}"

until vault status >/dev/null 2>&1; do
  echo "[vault-init] esperando Vault..."; sleep 2
done

vault secrets enable -path=secret kv-v2 >/dev/null 2>&1 || true
vault auth enable approle >/dev/null 2>&1 || true

write_policy() {
  svc="$1"
  vault policy write "${svc}-policy" - <<POLICY
path "secret/data/finanscore/${svc}" {
  capabilities = ["read"]
}
path "secret/data/finanscore/${svc}/*" {
  capabilities = ["read"]
}
path "secret/metadata/finanscore/${svc}" {
  capabilities = ["read", "list"]
}
path "secret/metadata/finanscore/${svc}/*" {
  capabilities = ["read", "list"]
}
POLICY
}

configure_approle() {
  svc="$1" role_id="$2" secret_id="$3"

  vault write "auth/approle/role/${svc}" \
    token_policies="${svc}-policy" \
    token_ttl=1h token_max_ttl=4h \
    secret_id_ttl=720h secret_id_num_uses=0 >/dev/null

  vault write "auth/approle/role/${svc}/role-id" role_id="$role_id" >/dev/null

  # Idempotente en reejecuciones contra el mismo Vault y correcto en Vault limpio.
  # Se ignora unicamente el caso conocido en que el SecretID ya esta registrado.
  set +e
  output="$(vault write "auth/approle/role/${svc}/custom-secret-id" secret_id="$secret_id" 2>&1)"
  rc=$?
  set -e

  if [ "$rc" -eq 0 ]; then
    echo "[vault-init] AppRole ${svc}: SecretID registrado."
  elif printf '%s' "$output" | grep -q "SecretID is already registered"; then
    echo "[vault-init] AppRole ${svc}: SecretID ya registrado; se conserva."
  else
    printf '%s\n' "$output" >&2
    echo "[vault-init] ERROR configurando AppRole ${svc}." >&2
    exit "$rc"
  fi
}

# Aplicacion: secretos almacenados en Vault; los servicios solo reciben AppRole.
vault kv put secret/finanscore/iam-service \
  DB_PASSWORD="${IAM_DB_PASSWORD}" \
  KAFKA_PASSWORD="${KAFKA_IAM_PASSWORD}" \
  KAFKA_TRUSTSTORE_PASSWORD="${DEV_CERT_PASSWORD}" \
  IAM_DEMO_PASSWORD="${IAM_DEMO_PASSWORD}" \
  IAM_ADMIN_PASSWORD="${IAM_ADMIN_PASSWORD}" \
  OAUTH2_NOTIFICATION_CLIENT_SECRET="${OAUTH2_NOTIFICATION_CLIENT_SECRET}" \
  GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-}" \
  TIKTOK_CLIENT_SECRET="${TIKTOK_CLIENT_SECRET:-}" \
  GMAIL_APP_PASSWORD="${GMAIL_APP_PASSWORD:-}" >/dev/null

vault kv put secret/finanscore/credit-service \
  DB_PASSWORD="${CREDIT_DB_PASSWORD}" KAFKA_PASSWORD="${KAFKA_CREDIT_PASSWORD}" KAFKA_TRUSTSTORE_PASSWORD="${DEV_CERT_PASSWORD}" >/dev/null
vault kv put secret/finanscore/payment-service \
  DB_PASSWORD="${PAYMENT_DB_PASSWORD}" KAFKA_PASSWORD="${KAFKA_PAYMENT_PASSWORD}" KAFKA_TRUSTSTORE_PASSWORD="${DEV_CERT_PASSWORD}" >/dev/null
vault kv put secret/finanscore/scoring-service \
  DB_PASSWORD="${SCORING_DB_PASSWORD}" KAFKA_PASSWORD="${KAFKA_SCORING_PASSWORD}" KAFKA_TRUSTSTORE_PASSWORD="${DEV_CERT_PASSWORD}" >/dev/null
vault kv put secret/finanscore/report-service \
  DB_PASSWORD="${REPORT_DB_PASSWORD}" KAFKA_PASSWORD="${KAFKA_REPORT_PASSWORD}" KAFKA_TRUSTSTORE_PASSWORD="${DEV_CERT_PASSWORD}" MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD}" >/dev/null
vault kv put secret/finanscore/notification-service \
  DB_PASSWORD="${NOTIFICATION_DB_PASSWORD}" KAFKA_PASSWORD="${KAFKA_NOTIFICATION_PASSWORD}" KAFKA_TRUSTSTORE_PASSWORD="${DEV_CERT_PASSWORD}" \
  OAUTH2_NOTIFICATION_CLIENT_SECRET="${OAUTH2_NOTIFICATION_CLIENT_SECRET}" GMAIL_APP_PASSWORD="${GMAIL_APP_PASSWORD:-}" MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD}" >/dev/null
vault kv put secret/finanscore/audit-service \
  DB_PASSWORD="${AUDIT_DB_PASSWORD}" KAFKA_PASSWORD="${KAFKA_AUDIT_PASSWORD}" KAFKA_TRUSTSTORE_PASSWORD="${DEV_CERT_PASSWORD}" >/dev/null
vault kv put secret/finanscore/query-service \
  DB_PASSWORD="${QUERY_DB_PASSWORD}" KAFKA_PASSWORD="${KAFKA_QUERY_PASSWORD}" KAFKA_TRUSTSTORE_PASSWORD="${DEV_CERT_PASSWORD}" REDIS_PASSWORD="${REDIS_PASSWORD}" >/dev/null

for svc in iam-service credit-service payment-service scoring-service report-service notification-service audit-service query-service; do
  write_policy "$svc"
done

configure_approle iam-service "$VAULT_IAM_ROLE_ID" "$VAULT_IAM_SECRET_ID"
configure_approle credit-service "$VAULT_CREDIT_ROLE_ID" "$VAULT_CREDIT_SECRET_ID"
configure_approle payment-service "$VAULT_PAYMENT_ROLE_ID" "$VAULT_PAYMENT_SECRET_ID"
configure_approle scoring-service "$VAULT_SCORING_ROLE_ID" "$VAULT_SCORING_SECRET_ID"
configure_approle report-service "$VAULT_REPORT_ROLE_ID" "$VAULT_REPORT_SECRET_ID"
configure_approle notification-service "$VAULT_NOTIFICATION_ROLE_ID" "$VAULT_NOTIFICATION_SECRET_ID"
configure_approle audit-service "$VAULT_AUDIT_ROLE_ID" "$VAULT_AUDIT_SECRET_ID"
configure_approle query-service "$VAULT_QUERY_ROLE_ID" "$VAULT_QUERY_SECRET_ID"

echo "[vault-init] KV + políticas + AppRole por servicio configurados. Root token no se distribuye a microservicios."
