# Motor de Scoring Distribuido V3.6.8.1

> Versión actual: seguimiento secuencial del workflow en Angular con detalle de fallos desde Query Service, manteniendo HTTPS interno, PostgreSQL TLS, Kong JWT, Kafka SASL_SSL, Vault y observabilidad. Ver `README_V3_6_8_WORKFLOW_UI.md`.

# Motor de Scoring Distribuido — V3.6.6

Versión actual del proyecto distribuido con Java 21, Spring Boot, Angular, Kong, Kafka, PostgreSQL, Vault, Redis, MinIO y observabilidad.

## Seguridad V3.6 / V3.6.6

- Angular → Kong: **HTTPS** (`https://localhost:8443`).
- Kafka: TLS + SASL/SCRAM + ACL.
- PostgreSQL: **TLS obligatorio** para TCP + `sslmode=verify-full` desde los microservicios.
- Sincronía interna real implementada: **Notification → IAM por gRPC + mTLS + OAuth2 Client Credentials**.
- IAM: login local, MFA, JWT, Google OAuth2/OIDC y TikTok OAuth2.
- Vault: AppRole por servicio y secretos fuera de `.env`.

## Inicio rápido

```powershell
Copy-Item .env.example .env
.\scripts\prepare-local-secrets.ps1
# complete los secretos OAuth/Gmail externos en secrets\bootstrap.env
.\scripts\generate-dev-certs.ps1
# PowerShell como Administrador:
.\scripts\install-dev-ca.ps1
.\scripts\run-local.ps1
```

Aplicación: `http://localhost:4200`  
API Gateway: `https://localhost:8443`

Lea `README_EJECUCION_LOCAL.md` y `README_V3_6_HTTPS_GRPC_MTLS_OAUTH2.md` antes de configurar Google/TikTok.


## Base consolidada V3.6.4

Consulte `README_V3_6_2_FIXES.md`. Esta revisión incorpora MinIO Community compilado localmente, versiones fijadas del stack de observabilidad, puertos host configurables, preflight de puertos y verificación automática de MinIO, Vault, Kong, Angular, Prometheus, Grafana, Loki, Tempo, OpenTelemetry Collector y Alloy.

## V3.6.5 - JWT en Kong + Spring Security

V3.6.5 agrega una capa JWT RS256 en Kong para rutas protegidas, manteniendo la validacion completa y autorizacion en cada microservicio. Consulte `README_V3_6_5_KONG_JWT.md`.


## V3.6.6 - PostgreSQL TLS

V3.6.6 agrega TLS obligatorio entre los microservicios y PostgreSQL. Las conexiones JDBC verifican CA y hostname con `sslmode=verify-full`; el servidor rechaza TCP sin SSL. Consulte `README_V3_6_6_POSTGRES_TLS.md`.
