# Implementation Status — V3.6.1

Implementado en fuente/configuración:

- 8 microservicios: IAM, Credit, Payment, Scoring, Report, Notification, Audit y Query.
- Database per Service.
- Kafka event-driven + Saga por coreografía + Transactional Outbox + consumidores idempotentes + DLT.
- Kafka TLS + SASL/SCRAM-SHA-512 + ACL.
- CQRS con Query Service, query_db, read models y Redis cache-aside.
- OpenTelemetry, OTel Collector, Tempo, Loki/Alloy, Prometheus y Grafana.
- HashiCorp Vault KV v2 + AppRole por microservicio; `.env` sin secretos.
- **Kong HTTPS/TLS en `8443`** y Angular usando `https://localhost:8443` como API base.
- **Google OAuth2/OIDC detrás de reverse proxy HTTPS** con forwarded headers.
- **gRPC real Notification → IAM** para directorio de usuarios.
- **mTLS obligatorio** en el canal gRPC.
- **OAuth2 Client Credentials** para autorización service-to-service con scope `iam.user.read`.
- Comprobación de identidad cruzada: CN de certificado mTLS = `sub` del JWT.
- Protobuf como contrato gRPC generado desde `contracts-java/src/main/proto`.

Pendiente de hardening productivo:

- Vault HA/persistente + TLS/auto-unseal/workload identity.
- PKI productiva y rotación automatizada de certificados/credenciales.
- Extender el patrón gRPC+mTLS+OAuth2 a nuevas llamadas síncronas cuando aparezcan; no sustituir Kafka en el workflow asíncrono.
- Validación runtime completa de Schema Registry y pruebas de caos/fallos parciales en infraestructura de integración.


## Hotfixes V3.6.1

- Scoring activa el perfil `postgres` desde Docker Compose.
- Vault AppRole init es idempotente sin ocultar errores distintos a SecretID duplicado.
- MinIO AIStor licenciado fue reemplazado por MinIO Community pinneado.
- Angular informa estado `FAILED` y errores de polling/sesión.
