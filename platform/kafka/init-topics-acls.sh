#!/usr/bin/env bash
set -euo pipefail

CLIENT=/tmp/admin.properties
BOOTSTRAP=kafka:29092

cat > "$CLIENT" <<PROPS
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-512
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="admin" password="${KAFKA_ADMIN_PASSWORD}";
ssl.truststore.location=/etc/kafka/secrets/kafka.truststore.jks
ssl.truststore.password=${KAFKA_SSL_TRUSTSTORE_PASSWORD}
ssl.truststore.type=JKS
ssl.endpoint.identification.algorithm=https
PROPS

until /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server "$BOOTSTRAP" --command-config "$CLIENT" >/dev/null 2>&1; do
  echo "Waiting for secured Kafka..."
  sleep 2
done

# Ensure the CQRS query-service SCRAM credential exists even when the Kafka data volume
# was created by V3.2. This keeps upgrades from requiring a broker volume reset.
/opt/kafka/bin/kafka-configs.sh --bootstrap-server "$BOOTSTRAP" --command-config "$CLIENT" --alter \
  --entity-type users --entity-name query-service \
  --add-config "SCRAM-SHA-512=[iterations=8192,password=${KAFKA_QUERY_PASSWORD}]" >/dev/null

events=(
  credit.request.created.v1
  payment.validated.v1
  payment.rejected.v1
  credit.evaluation.requested.v1
  scoring.calculated.v1
  scoring.failed.v1
  report.generated.v1
  report.failed.v1
  notification.sent.v1
  notification.failed.v1
  iam.audit.v1
)

for topic in "${events[@]}"; do
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --command-config "$CLIENT" \
    --create --if-not-exists --topic "$topic" --partitions 3 --replication-factor 1
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --command-config "$CLIENT" \
    --create --if-not-exists --topic "$topic.DLT" --partitions 3 --replication-factor 1
 done

/opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --command-config "$CLIENT" \
  --create --if-not-exists --topic _schemas --partitions 1 --replication-factor 1 --config cleanup.policy=compact

acl_topic() {
  local principal=$1 topic=$2; shift 2
  /opt/kafka/bin/kafka-acls.sh --bootstrap-server "$BOOTSTRAP" --command-config "$CLIENT" --add \
    --allow-principal "User:${principal}" --topic "$topic" "$@" >/dev/null
}
acl_group() {
  local principal=$1 group=$2; shift 2
  /opt/kafka/bin/kafka-acls.sh --bootstrap-server "$BOOTSTRAP" --command-config "$CLIENT" --add \
    --allow-principal "User:${principal}" --group "$group" "$@" >/dev/null
}
acl_cluster() {
  local principal=$1; shift
  /opt/kafka/bin/kafka-acls.sh --bootstrap-server "$BOOTSTRAP" --command-config "$CLIENT" --add \
    --allow-principal "User:${principal}" --cluster "$@" >/dev/null
}
producer_topic() {
  local principal=$1 topic=$2
  acl_topic "$principal" "$topic" --operation Write --operation Describe
}
consumer_topic() {
  local principal=$1 topic=$2
  acl_topic "$principal" "$topic" --operation Read --operation Describe
}
producer_principal() {
  acl_cluster "$1" --operation IdempotentWrite --operation Describe
}

# IAM: only publishes IAM audit events.
producer_principal iam-service
producer_topic iam-service iam.audit.v1

# Credit: owns the request workflow, publishes request/evaluation events and consumes workflow results.
producer_principal credit-service
producer_topic credit-service credit.request.created.v1
producer_topic credit-service credit.evaluation.requested.v1
for t in payment.validated.v1 payment.rejected.v1 scoring.calculated.v1 report.generated.v1 notification.sent.v1; do
  consumer_topic credit-service "$t"
  producer_topic credit-service "$t.DLT"
done
for g in credit-payment-validated credit-payment-rejected credit-scoring-result credit-report-result credit-notification-result; do
  acl_group credit-service "$g" --operation Read --operation Describe
done
for t in payment.validated.v1.DLT credit.evaluation.requested.v1.DLT scoring.calculated.v1.DLT report.generated.v1.DLT notification.sent.v1.DLT; do
  consumer_topic credit-service "$t"
done
acl_group credit-service credit-workflow-dlt --operation Read --operation Describe

# Payment: REST-driven service; publishes payment outcome only.
producer_principal payment-service
producer_topic payment-service payment.validated.v1
producer_topic payment-service payment.rejected.v1

# Scoring: consumes the evaluation request, publishes result and can send failures to its DLT.
producer_principal scoring-service
consumer_topic scoring-service credit.evaluation.requested.v1
producer_topic scoring-service credit.evaluation.requested.v1.DLT
producer_topic scoring-service scoring.calculated.v1
producer_topic scoring-service scoring.failed.v1
acl_group scoring-service scoring-calculation --operation Read --operation Describe

# Report: consumes scoring result and publishes report result.
producer_principal report-service
consumer_topic report-service scoring.calculated.v1
producer_topic report-service scoring.calculated.v1.DLT
producer_topic report-service report.generated.v1
producer_topic report-service report.failed.v1
acl_group report-service report-generation --operation Read --operation Describe

# Notification: consumes report result and publishes final notification result.
producer_principal notification-service
consumer_topic notification-service report.generated.v1
producer_topic notification-service report.generated.v1.DLT
producer_topic notification-service notification.sent.v1
producer_topic notification-service notification.failed.v1
acl_group notification-service notification-email --operation Read --operation Describe

# Audit: reads every business event and every DLT. If audit processing itself fails,
# its error handler may write the corresponding DLT.
producer_principal audit-service
for t in "${events[@]}"; do
  consumer_topic audit-service "$t"
  consumer_topic audit-service "$t.DLT"
  producer_topic audit-service "$t.DLT"
done
acl_group audit-service audit-service --operation Read --operation Describe
acl_group audit-service audit-dlt-indexer --operation Read --operation Describe

# Query Service: CQRS read-side projections. Read-only access to workflow events and DLTs.
for t in credit.request.created.v1 payment.validated.v1 payment.rejected.v1 credit.evaluation.requested.v1 scoring.calculated.v1 scoring.failed.v1 report.generated.v1 report.failed.v1 notification.sent.v1 notification.failed.v1; do
  consumer_topic query-service "$t"
  consumer_topic query-service "$t.DLT"
done
acl_group query-service query-service-projections --operation Read --operation Describe
acl_group query-service query-service-projections-dlt --operation Read --operation Describe

# Schema Registry has a dedicated principal and least-privilege access to _schemas.
consumer_topic schema-registry _schemas
producer_topic schema-registry _schemas
acl_topic schema-registry _schemas --operation DescribeConfigs
acl_group schema-registry schema-registry --operation Read --operation Describe
acl_cluster schema-registry --operation IdempotentWrite --operation Describe

printf '\nKafka TLS + SCRAM-SHA-512 + ACL initialization complete.\n'
/opt/kafka/bin/kafka-acls.sh --bootstrap-server "$BOOTSTRAP" --command-config "$CLIENT" --list
