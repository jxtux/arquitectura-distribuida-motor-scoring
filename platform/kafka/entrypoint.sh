#!/usr/bin/env bash
set -euo pipefail

CONFIG=/opt/kafka/config/finanscore-server.properties
DATA_DIR=/var/lib/kafka/data
mkdir -p "$DATA_DIR" /etc/kafka/client

required=(
  KAFKA_CLUSTER_ID KAFKA_SSL_KEYSTORE_PASSWORD KAFKA_SSL_KEY_PASSWORD
  KAFKA_SSL_TRUSTSTORE_PASSWORD KAFKA_ADMIN_PASSWORD KAFKA_BROKER_PASSWORD
  KAFKA_IAM_PASSWORD KAFKA_CREDIT_PASSWORD KAFKA_PAYMENT_PASSWORD
  KAFKA_SCORING_PASSWORD KAFKA_REPORT_PASSWORD KAFKA_NOTIFICATION_PASSWORD
  KAFKA_AUDIT_PASSWORD KAFKA_QUERY_PASSWORD KAFKA_SCHEMA_REGISTRY_PASSWORD
)
for v in "${required[@]}"; do
  if [[ -z "${!v:-}" ]]; then
    echo "ERROR: required environment variable $v is empty" >&2
    exit 1
  fi
done

for f in /etc/kafka/secrets/kafka.keystore.jks /etc/kafka/secrets/kafka.truststore.jks; do
  [[ -f "$f" ]] || { echo "ERROR: missing Kafka TLS file $f. Run ./scripts/generate-dev-certs.sh first." >&2; exit 1; }
done

cat > "$CONFIG" <<PROPS
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@kafka:9093
controller.listener.names=CONTROLLER
listeners=SASL_SSL_INTERNAL://0.0.0.0:29092,SASL_SSL_EXTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
advertised.listeners=SASL_SSL_INTERNAL://kafka:29092,SASL_SSL_EXTERNAL://localhost:9092
listener.security.protocol.map=SASL_SSL_INTERNAL:SASL_SSL,SASL_SSL_EXTERNAL:SASL_SSL,CONTROLLER:SSL
inter.broker.listener.name=SASL_SSL_INTERNAL
early.start.listeners=CONTROLLER
log.dirs=$DATA_DIR
num.partitions=3
default.replication.factor=1
min.insync.replicas=1
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1
group.initial.rebalance.delay.ms=0

ssl.keystore.location=/etc/kafka/secrets/kafka.keystore.jks
ssl.keystore.password=${KAFKA_SSL_KEYSTORE_PASSWORD}
ssl.key.password=${KAFKA_SSL_KEY_PASSWORD}
ssl.truststore.location=/etc/kafka/secrets/kafka.truststore.jks
ssl.truststore.password=${KAFKA_SSL_TRUSTSTORE_PASSWORD}
ssl.client.auth=none
listener.name.controller.ssl.client.auth=required

sasl.enabled.mechanisms=SCRAM-SHA-512
sasl.mechanism.inter.broker.protocol=SCRAM-SHA-512
listener.name.sasl_ssl_internal.scram-sha-512.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="kafka" password="${KAFKA_BROKER_PASSWORD}";
listener.name.sasl_ssl_external.scram-sha-512.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required;

authorizer.class.name=org.apache.kafka.metadata.authorizer.StandardAuthorizer
super.users=User:admin;User:kafka;User:CN=kafka
allow.everyone.if.no.acl.found=false
PROPS

cat > /etc/kafka/client/admin.properties <<PROPS
security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-512
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="admin" password="${KAFKA_ADMIN_PASSWORD}";
ssl.truststore.location=/etc/kafka/secrets/kafka.truststore.jks
ssl.truststore.password=${KAFKA_SSL_TRUSTSTORE_PASSWORD}
ssl.truststore.type=JKS
ssl.endpoint.identification.algorithm=https
PROPS

if [[ ! -f "$DATA_DIR/meta.properties" ]]; then
  echo "Formatting KRaft metadata and creating SCRAM-SHA-512 principals..."
  /opt/kafka/bin/kafka-storage.sh format --ignore-formatted \
    -t "${KAFKA_CLUSTER_ID}" -c "$CONFIG" \
    --add-scram "SCRAM-SHA-512=[name=admin,password=${KAFKA_ADMIN_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=kafka,password=${KAFKA_BROKER_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=iam-service,password=${KAFKA_IAM_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=credit-service,password=${KAFKA_CREDIT_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=payment-service,password=${KAFKA_PAYMENT_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=scoring-service,password=${KAFKA_SCORING_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=report-service,password=${KAFKA_REPORT_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=notification-service,password=${KAFKA_NOTIFICATION_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=audit-service,password=${KAFKA_AUDIT_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=query-service,password=${KAFKA_QUERY_PASSWORD}]" \
    --add-scram "SCRAM-SHA-512=[name=schema-registry,password=${KAFKA_SCHEMA_REGISTRY_PASSWORD}]"
fi

exec /opt/kafka/bin/kafka-server-start.sh "$CONFIG"
