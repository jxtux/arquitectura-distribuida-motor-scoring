# V3.5 — Secret Management realista con HashiCorp Vault

## Objetivo
V3.5 elimina contraseñas y client secrets del `.env`. El archivo `.env` queda reservado a configuración no sensible. Los secretos locales de bootstrap se generan en `secrets/bootstrap.env`, archivo excluido de Git, y `vault-init` los registra en HashiCorp Vault KV v2.

## Flujo

```text
.env (no sensible)
          \
           +--> Docker Compose
          /
secrets/bootstrap.env (local, ignorado por Git)
              |
              +--> infraestructura que necesita credenciales al arrancar
              |    PostgreSQL / Kafka / Redis / MinIO / Vault
              |
              +--> vault-init (único componente con root token de desarrollo)
                         |
                         +--> secret/finanscore/<service>
                         +--> policies de mínimo privilegio
                         +--> AppRole por microservicio

microservicio --> AppRole login --> token temporal Vault --> KV propio
```

## Cambios respecto a V3.4

- `.env.example` ya no contiene passwords, tokens ni OAuth client secrets.
- Nuevo `secrets/bootstrap.env`, generado localmente y excluido de Git.
- Spring Cloud Vault cambia de `TOKEN` estático a `APPROLE`.
- Cada microservicio tiene un `role_id` y `secret_id` distinto.
- El root token de Vault solo llega a `vault` y `vault-init`, nunca a los microservicios.
- Las políticas permiten a cada servicio leer únicamente `secret/finanscore/<su-servicio>`.
- Las contraseñas de las bases de datos por servicio también se generan y se cargan en Vault; ya no son `iam_user`, `credit_user`, etc.
- Kafka, Redis, MinIO, PostgreSQL y Grafana reciben sus credenciales de bootstrap local, no del `.env` versionable.
- Se incluyen scripts PowerShell y Bash para generar secretos y levantar el stack.

## Ejecución recomendada en Windows PowerShell

```powershell
Copy-Item .env.example .env
.\scripts\prepare-local-secrets.ps1
.\scripts\generate-dev-certs.ps1

docker compose --env-file .env --env-file secrets/bootstrap.env config
docker compose --env-file .env --env-file secrets/bootstrap.env up --build -d
```

O de forma automatizada:

```powershell
.\scripts\run-local.ps1
```

Para detener y eliminar volúmenes del proyecto:

```powershell
.\scripts\stop-local.ps1
```

## OAuth / Gmail
Después de generar `secrets/bootstrap.env`, coloque allí únicamente en su equipo local los valores reales si quiere habilitarlos:

```text
GOOGLE_CLIENT_SECRET=...
TIKTOK_CLIENT_SECRET=...
GMAIL_APP_PASSWORD=...
```

Los identificadores no secretos (`GOOGLE_CLIENT_ID`, `TIKTOK_CLIENT_KEY`, `GMAIL_USERNAME`) permanecen en `.env`.

## Producción
Esta entrega conserva Vault `dev mode` para la demo Docker Compose. En producción: Vault server mode con storage persistente, TLS, auto-unseal/operación segura, autenticación de workloads (AppRole con entrega segura, Kubernetes auth, cloud IAM o Vault Agent) y secretos iniciales suministrados por CI/CD o el secret manager del entorno. No se debe conservar `secrets/bootstrap.env` en hosts productivos como mecanismo permanente.
