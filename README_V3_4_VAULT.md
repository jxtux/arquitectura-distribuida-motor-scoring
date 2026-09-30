# Motor Scoring Distribuido V3.4 — HashiCorp Vault integrado

## Objetivo

La V3.4 convierte HashiCorp Vault de componente arquitectónico/documentado en un componente realmente consumido por los microservicios. Los servicios Spring Boot cargan secretos desde Vault mediante Spring Cloud Vault y la Config Data API.

## Qué cambia

- Se añade `spring-cloud-starter-vault-config` a IAM, Credit, Payment, Scoring, Report, Notification, Audit y Query Service.
- Cada servicio lee un contexto KV v2 independiente: `secret/finanscore/<service>`.
- Docker Compose incorpora `vault-init`, que crea los secretos de demo, políticas de mínimo privilegio y un token distinto por microservicio.
- Los microservicios dejan de recibir directamente por `environment` las contraseñas de BD, credenciales Kafka, contraseña de Redis y otros secretos de aplicación que ahora provienen de Vault.
- Actuator puede exponer el estado de Vault dentro del health del servicio.

## Secretos centralizados en Vault

Según el servicio, se almacenan:

- contraseñas PostgreSQL;
- credenciales SASL/SCRAM de Kafka;
- contraseña del truststore TLS de Kafka;
- contraseña de Redis del Query Service;
- contraseña de MinIO para Report/Notification;
- `IAM_INTERNAL_TOKEN`;
- contraseña del administrador/demo;
- secretos OAuth de Google y TikTok;
- Gmail App Password.

Los certificados y claves PEM del entorno local siguen montándose desde `platform/certs`. La evolución recomendada para producción es usar Vault PKI/Vault Agent o identidad de workload en vez de archivos y tokens estáticos.

## Aislamiento por servicio

Cada microservicio dispone de una política que solo permite leer su propio contexto:

```text
iam-service          -> secret/finanscore/iam-service
credit-service       -> secret/finanscore/credit-service
payment-service      -> secret/finanscore/payment-service
scoring-service      -> secret/finanscore/scoring-service
report-service       -> secret/finanscore/report-service
notification-service -> secret/finanscore/notification-service
audit-service        -> secret/finanscore/audit-service
query-service        -> secret/finanscore/query-service
```

Esto implementa Least Privilege también para el acceso a secretos.

## Flujo de arranque local

```text
.env
  ↓ solo bootstrap de infraestructura/demo
Vault
  ↓
vault-init
  ├─ KV v2
  ├─ políticas
  └─ tokens por servicio
       ↓
Spring Cloud Vault
       ↓
Microservicios
```

Ejecutar:

```bash
cp .env.example .env
./scripts/generate-dev-certs.sh
docker compose up --build
```

## Producción

Los tokens estáticos definidos en `.env.example` son únicamente para la demo Docker Compose. En producción se recomienda AppRole con SecretID de corta vida, Vault Agent, Kubernetes Auth o una identidad equivalente del entorno de ejecución. El root token nunca debe entregarse a los microservicios.
