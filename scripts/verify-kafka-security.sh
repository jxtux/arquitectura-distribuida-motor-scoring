#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

for f in platform/certs/ca.crt platform/certs/client.truststore.jks; do
  [[ -f "$f" ]] || { echo "Missing $f. Run ./scripts/generate-dev-certs.sh first." >&2; exit 1; }
done

command -v docker >/dev/null || { echo "Docker is required." >&2; exit 1; }

echo '== Broker security properties =='
docker compose --env-file .env --env-file secrets/bootstrap.env exec -T kafka sh -lc "grep -E '^(listeners|advertised.listeners|listener.security.protocol.map|sasl.enabled.mechanisms|sasl.mechanism.inter.broker.protocol|authorizer.class.name|allow.everyone.if.no.acl.found)=' /opt/kafka/config/finanscore-server.properties"

echo
echo '== ACLs =='
docker compose --env-file .env --env-file secrets/bootstrap.env exec -T kafka /opt/kafka/bin/kafka-acls.sh \
  --bootstrap-server kafka:29092 \
  --command-config /etc/kafka/client/admin.properties \
  --list

echo
echo '== query-service SCRAM credential =='
docker compose --env-file .env --env-file secrets/bootstrap.env exec -T kafka /opt/kafka/bin/kafka-configs.sh \
  --bootstrap-server kafka:29092 --command-config /etc/kafka/client/admin.properties \
  --describe --entity-type users --entity-name query-service

echo
echo '== TLS certificate handshake on external listener =='
openssl s_client -connect localhost:9092 -servername localhost -CAfile platform/certs/ca.crt </dev/null 2>/dev/null \
  | openssl x509 -noout -subject -issuer -dates

echo
echo 'Kafka security verification finished.'
