#!/usr/bin/env bash
set -euo pipefail

CERT_SOURCE="/run/secrets"
TLS_DIR="/var/lib/postgresql/tls"
SERVER_CERT="${CERT_SOURCE}/postgres-server.crt"
SERVER_KEY="${CERT_SOURCE}/postgres-server.key"
CA_CERT="${CERT_SOURCE}/ca.crt"

for f in "$SERVER_CERT" "$SERVER_KEY" "$CA_CERT"; do
  if [[ ! -s "$f" ]]; then
    echo "ERROR: falta material TLS requerido: $f" >&2
    exit 1
  fi
done

# PostgreSQL exige permisos restrictivos en la clave privada. En Docker Desktop,
# un bind mount de Windows puede exponer permisos demasiado abiertos; por eso
# copiamos el material al volumen/directorio Linux antes de iniciar el servidor.
mkdir -p "$TLS_DIR"
cp "$SERVER_CERT" "$TLS_DIR/server.crt"
cp "$SERVER_KEY" "$TLS_DIR/server.key"
cp "$CA_CERT" "$TLS_DIR/ca.crt"
chown -R postgres:postgres "$TLS_DIR"
chmod 700 "$TLS_DIR"
chmod 600 "$TLS_DIR/server.key"
chmod 644 "$TLS_DIR/server.crt" "$TLS_DIR/ca.crt"

exec /usr/local/bin/docker-entrypoint.sh postgres \
  -c ssl=on \
  -c ssl_cert_file="$TLS_DIR/server.crt" \
  -c ssl_key_file="$TLS_DIR/server.key" \
  -c ssl_ca_file="$TLS_DIR/ca.crt" \
  -c ssl_min_protocol_version='TLSv1.2' \
  -c hba_file=/opt/finanscore/pg_hba_tls.conf
