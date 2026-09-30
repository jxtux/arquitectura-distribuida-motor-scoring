# Apache Kafka - TLS + SASL/SCRAM + ACL

## Flujo de seguridad

```text
Microservicio
   │
   │ TLS: cifra y valida al broker
   │ SASL/SCRAM-SHA-512: autentica el principal
   ▼
Kafka
   │
   └─ ACL: autoriza solamente topics y grupos permitidos
```

Kafka expone `kafka:29092` para clientes Docker y `localhost:9092` para clientes del host. Ambos usan `SASL_SSL`.

## Principales

| Principal | Produce | Consume |
|---|---|---|
| `iam-service` | `iam.audit.v1` | - |
| `credit-service` | `credit.request.created.v1`, `credit.evaluation.requested.v1`, DLT técnicos de sus listeners | resultados de Payment/Scoring/Report/Notification y DLT del workflow |
| `payment-service` | `payment.validated.v1`, `payment.rejected.v1` | - |
| `scoring-service` | `scoring.calculated.v1`, DLT del request de scoring | `credit.evaluation.requested.v1` |
| `report-service` | `report.generated.v1`, DLT de scoring | `scoring.calculated.v1` |
| `notification-service` | `notification.sent.v1`, DLT de reporte | `report.generated.v1` |
| `audit-service` | DLT técnico si falla el indexado | todos los eventos de negocio y todos los DLT |
| `query-service` | - | eventos del workflow y sus DLT para construir Read Models |
| `schema-registry` | `_schemas` | `_schemas` |

Los productores con idempotencia reciben además `IdempotentWrite` a nivel de cluster. `admin` y `kafka` son superusuarios de infraestructura.

## Archivos

- `platform/kafka/Dockerfile`
- `platform/kafka/entrypoint.sh`: configura KRaft, TLS, SCRAM y `StandardAuthorizer`.
- `platform/kafka/init-topics-acls.sh`: crea topics y ACL.
- `scripts/generate-dev-certs.sh`: genera CA, keystore y truststores de desarrollo.

## Arranque

```bash
cp .env.example .env
# Cambiar las contraseñas del .env
./scripts/generate-dev-certs.sh
docker compose up --build
```

Si se cambian credenciales SCRAM después de que `kafka_data` ya fue inicializado, en laboratorio se debe recrear el volumen Kafka o rotar las credenciales explícitamente con herramientas administrativas de Kafka.


V3.3 crea/actualiza explícitamente la credencial SCRAM de `query-service` desde `kafka-init`, por lo que puede agregarse sobre un volumen Kafka proveniente de V3.2 sin reformatear KRaft.
