# Motor de Scoring Distribuido V3.6 — HTTPS + gRPC mTLS + OAuth2 interno

Esta versión parte de V3.5 y corrige las desviaciones detectadas durante la ejecución local.

## Cambios principales

1. **Angular → Kong por HTTPS**: `API_BASE_URL=https://localhost:8443`.
2. **Kong TLS real**: listener `8443 ssl`, certificado `kong-server.crt/key` emitido por la CA local.
3. **Google OAuth2/OIDC detrás de Kong**: IAM usa `server.forward-headers-strategy=framework`, de modo que Spring respeta `X-Forwarded-Proto/Host/Port` y construye el callback público HTTPS.
4. **TikTok OAuth2**: mantiene callback público por ngrok, ahora apuntando al Kong HTTPS.
5. **gRPC interno real**: Notification Service consulta el directorio de usuarios de IAM por gRPC en `iam-service:9091`.
6. **mTLS real en gRPC**: IAM exige certificado cliente firmado por la CA; Notification usa `notification-grpc-client.crt/key`.
7. **OAuth2 Client Credentials interno**: Notification obtiene un access token corto en `POST /oauth2/token`; el JWT lleva `scope=iam.user.read`, `aud=iam-grpc` y `token_use=CLIENT_CREDENTIALS`.
8. **Defense in depth**: el servidor gRPC verifica que el `sub` del JWT coincida con el CN del certificado mTLS (`notification-service`).
9. Se elimina el adaptador temporal REST con `X-Internal-Token`.
10. Se mantienen Kafka TLS + SASL/SCRAM + ACL, Vault AppRole, CQRS/Redis, Outbox, DLT y observabilidad.

## Correcciones de ejecución incorporadas

- `OperationQueryService.java`: `Timer` de Micrometer importado explícitamente; se elimina la ambigüedad con `java.util.Timer`.
- Dockerfiles: los JAR ya no están fijados a `3.3.0`; usan `*-service-*.jar`.
- V3.6.2 mantiene MinIO Community `RELEASE.2025-09-07T16-13-09Z`, pero lo compila localmente desde el código fuente oficial mediante `platform/minio/Dockerfile`, evitando la dependencia de una imagen precompilada de `quay.io`.
- `stop-local.ps1` ya no ejecuta `down -v`; los volúmenes se conservan.
- `run-local.ps1` verifica los nuevos certificados TLS/gRPC y el secreto OAuth2 interno.

## Flujo externo

```text
Angular http://localhost:4200
       |
       | HTTPS + JWT
       v
Kong https://localhost:8443
       |
       +--> IAM / Credit / Payment / Query / Report / Audit
```

El frontend puede seguir servido por HTTP en localhost; **las llamadas API van por HTTPS**.

## Flujo síncrono interno implementado

```text
Notification Service
   | 1. HTTPS OAuth2 Client Credentials
   +----------------------------> Kong :8443 -> IAM /oauth2/token
   |
   | 2. gRPC + mTLS + Bearer JWT(scope=iam.user.read)
   +----------------------------> IAM gRPC :9091
```

El workflow principal Credit → Payment → Scoring → Report → Notification continúa siendo **asíncrono por Kafka**. gRPC se reserva para llamadas síncronas puntuales.

## Google OAuth2/OIDC

No se agrega `GOOGLE_REDIRECT_URI` obligatorio. Spring calcula el callback usando los headers reenviados por Kong.

En Google Cloud, registre exactamente:

```text
https://localhost:8443/login/oauth2/code/google
```

Antes debe instalar la CA local en Windows para que el navegador confíe en `https://localhost:8443`.

## TikTok

En `.env`:

```text
TIKTOK_REDIRECT_URI=https://TU-DOMINIO.ngrok-free.app/api/v1/auth/social/tiktok/callback
```

Inicie ngrok contra el Kong HTTPS:

```powershell
ngrok http https://localhost:8443
```

La URL configurada en TikTok Developers debe coincidir exactamente con `TIKTOK_REDIRECT_URI`.

## Secretos nuevos

V3.6 reemplaza `IAM_INTERNAL_TOKEN` por:

```text
OAUTH2_NOTIFICATION_CLIENT_SECRET
```

Se genera en `secrets/bootstrap.env`, se carga a Vault y solo se entrega a IAM y Notification mediante sus AppRole/contextos KV.

## Nota de producción

Los certificados y la CA generados por los scripts son solo para desarrollo. En producción: certificados emitidos por PKI corporativa/AC pública según el caso, Vault server mode con TLS/persistencia y entrega de identidad/secreto mediante workload identity o mecanismo equivalente.

## Migrar una configuración local V3.5 existente

Si copia su `.env` y `secrets/bootstrap.env` desde V3.5, ejecute:

```powershell
.\scripts\migrate-v35-local-config.ps1
```

El script conserva los valores existentes, activa cookie segura para el API HTTPS, agrega el secreto OAuth2 interno si falta y regenera los certificados V3.6.
