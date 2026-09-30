# Motor de Scoring Distribuido V3.6.2 — estabilización local y observabilidad

Esta versión parte de V3.6.1 e incorpora los ajustes detectados durante el arranque real en Windows con Docker Desktop.

## Cambios incluidos

1. **MinIO Community sin dependencia de Quay**: `minio` se construye localmente con `platform/minio/Dockerfile` usando el tag fuente `RELEASE.2025-09-07T16-13-09Z`. La imagen local se llama `finanscore-minio:RELEASE.2025-09-07T16-13-09Z`.
2. **Puertos host configurables**: todos los puertos publicados relevantes se parametrizan con variables `*_HOST_PORT` en `.env`. Los puertos internos entre contenedores no cambian.
3. **Preflight de puertos**: `scripts/preflight-local.ps1` detecta antes del `docker compose up` si otro contenedor o proceso ocupa PostgreSQL, Kafka, MinIO, Vault, Kong, Angular u observabilidad. Esto evita arranques parciales como el fallo de Prometheus por `9090 already allocated`.
4. **Observabilidad sin `latest`**: se fijan versiones reproducibles para reducir roturas por cambios incompatibles:
   - Prometheus `v3.14.0`
   - Grafana `13.2.2`
   - Loki `3.7.8`
   - Tempo `2.10.8`
   - OpenTelemetry Collector Contrib `0.161.0`
   - Grafana Alloy `v1.20.0`
5. **Tempo estabilizado en la rama 2.x mantenida**: se evita `grafana/tempo:latest`, ya que Tempo 3.x introduce cambios mayores de arquitectura/configuración. La configuración local existente es de estilo 2.x.
6. **Reinicio de observabilidad**: MinIO, Prometheus, Grafana, Loki, Tempo, OTel Collector y Alloy usan `restart: unless-stopped`.
7. **Verificación automática**: `scripts/verify-local-stack.ps1` prueba endpoints HTTP de MinIO, Vault, Kong Admin, Angular, Prometheus, Grafana, Loki, Tempo, OTel Collector y Alloy y muestra también `docker compose ps -a`.
8. **Proyecto Compose aislado**: el proyecto pasa a `motor-scoring-distributed-v3-6-2`, por lo que sus contenedores y volúmenes quedan separados de V3.6/V3.6.1.

## Primera ejecución en Windows

```powershell
Copy-Item .env.example .env
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\prepare-local-secrets.ps1
# Complete GOOGLE_CLIENT_SECRET / TIKTOK_CLIENT_SECRET / GMAIL_APP_PASSWORD si aplica.
.\scripts\generate-dev-certs.ps1
# PowerShell como administrador, una sola vez por CA local:
.\scripts\install-dev-ca.ps1
.\scripts\run-local.ps1
```

`run-local.ps1` ejecuta el preflight, valida Compose, levanta el stack y finalmente ejecuta la verificación HTTP.

## Si una versión anterior sigue levantada

La opción recomendada es apagarla antes de iniciar V3.6.2. Por ejemplo, desde la carpeta de la versión anterior:

```powershell
docker compose down
```

No use `-v` si desea conservar sus volúmenes.

También puede cambiar un puerto host en `.env`, por ejemplo:

```env
PROMETHEUS_HOST_PORT=19090
GRAFANA_HOST_PORT=13000
```

Esto no modifica los puertos internos `prometheus:9090` ni `grafana:3000` usados dentro de la red Docker.

## Verificación manual

```powershell
.\scripts\preflight-local.ps1
.\scripts\verify-local-stack.ps1

docker compose --env-file .env --env-file secrets/bootstrap.env ps -a
```

Accesos por defecto:

- Angular: `http://localhost:4200`
- Kong HTTPS: `https://localhost:8443`
- MinIO Console: `http://localhost:9001`
- Vault: `http://localhost:8200`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Loki readiness: `http://localhost:3100/ready`
- Tempo readiness: `http://localhost:3200/ready`
- Alloy: `http://localhost:12345`

## Variables externas personalizadas

Configuración no sensible en `.env`:

```env
IAM_MAIL_FROM=
GMAIL_USERNAME=
GOOGLE_CLIENT_ID=
TIKTOK_CLIENT_KEY=
TIKTOK_REDIRECT_URI=
```

Secretos externos en `secrets/bootstrap.env`:

```env
GOOGLE_CLIENT_SECRET=
TIKTOK_CLIENT_SECRET=
GMAIL_APP_PASSWORD=
```

No elimine las demás variables generadas automáticamente en `secrets/bootstrap.env`.

## Migrar desde V3.6.1

La nueva distribución no incluye sus secretos personales. Si ya tiene V3.6.1 configurada, puede copiar de forma privada desde su instalación anterior:

- `.env`
- `secrets/bootstrap.env`
- opcionalmente `platform/certs` si desea reutilizar la misma CA/certificados

Si copia un `.env` de V3.6.1, las nuevas variables `*_HOST_PORT` son opcionales porque Compose conserva los puertos anteriores como valores por defecto.
