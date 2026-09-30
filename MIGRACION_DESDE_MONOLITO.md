# Migración desde el proyecto original

## Reutilizado

- Java 21 / Spring Boot y dominio puro de scoring.
- IAM local: registro, verificación, MFA, JWT, roles/scopes, Google OAuth2/OIDC, TikTok OAuth2 y Gmail SMTP.
- Transactional Outbox, idempotencia y conceptos Kafka.
- La configuración segura Kafka del monolito sirvió como base para V3.2.
- Angular y pantallas de autenticación.

## Cambios estructurales

- El despliegue único se divide en 7 aplicaciones Spring Boot independientes.
- Database per Service: `iam_db`, `credit_db`, `payment_db`, `scoring_db`, `report_db`, `notification_db`, `audit_db`.
- Payment, Scoring, Report y Notification se separan por responsabilidad.
- MinIO almacena PDF; Kafka transporta metadata/referencias.
- Audit Service y observabilidad se separan conceptualmente y físicamente.
- V3.1 agregó observabilidad y `/admin`; V3.2 activó Kafka seguro; V3.3 agrega CQRS formal con Query Service, `query_db`, Read Models y Redis.

## Seguridad Kafka recuperada y adaptada

La seguridad original se adapta al nuevo reparto de responsabilidades:

- TLS/SSL para cifrado y autenticación del broker.
- SASL/SCRAM-SHA-512 con un principal por microservicio.
- ACL explícita según los topics y consumer groups que cada servicio realmente utiliza.
- Principal separado para Schema Registry.

## Hardening aún pendiente

1. Vault/Secret Manager real para sustituir `.env` y material local.
2. Notification→IAM por gRPC + mTLS + OAuth2 Client Credentials.
3. Enforcement de contratos mediante Schema Registry en runtime.
4. Testcontainers/CI y topología Kafka HA multi-broker para producción.
