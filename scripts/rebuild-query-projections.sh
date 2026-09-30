#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
command -v docker >/dev/null || { echo 'Docker is required.' >&2; exit 1; }

echo 'Stopping Query Service so Kafka consumer groups become inactive...'
docker compose --env-file .env --env-file secrets/bootstrap.env stop query-service

echo 'Clearing CQRS read models and idempotency guard...'
docker compose --env-file .env --env-file secrets/bootstrap.env exec -T postgres psql -U postgres -d query_db -v ON_ERROR_STOP=1 -c \
  'TRUNCATE TABLE processed_projection_event, user_request_read_model, operation_read_model;'

echo 'Clearing Redis query cache...'
docker compose --env-file .env --env-file secrets/bootstrap.env exec -T redis sh -lc 'redis-cli -a "$REDIS_PASSWORD" FLUSHDB >/dev/null'

echo 'Resetting Kafka projection groups to earliest retained offsets...'
for group in query-service-projections query-service-projections-dlt; do
  docker compose --env-file .env --env-file secrets/bootstrap.env exec -T kafka /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server kafka:29092 --command-config /etc/kafka/client/admin.properties \
    --group "$group" --reset-offsets --to-earliest --all-topics --execute || true
done

echo 'Starting Query Service. It will replay retained events and rebuild the read models.'
docker compose --env-file .env --env-file secrets/bootstrap.env start query-service
