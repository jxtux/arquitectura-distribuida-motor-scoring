# Ejecución local — V3.6.5

## 1. Preparar configuración

```powershell
Copy-Item .env.example .env
.\scripts\prepare-local-secrets.ps1
```

Edite `.env` con valores **no sensibles** (`GOOGLE_CLIENT_ID`, `TIKTOK_CLIENT_KEY`, `TIKTOK_REDIRECT_URI`, Gmail username/from).

Edite `secrets\bootstrap.env` y coloque únicamente los secretos externos que corresponda:

```text
GOOGLE_CLIENT_SECRET=...
TIKTOK_CLIENT_SECRET=...
GMAIL_APP_PASSWORD=...
```

Los demás secretos se generan automáticamente.

## 2. Generar certificados

```powershell
.\scripts\generate-dev-certs.ps1
```

V3.6.5 genera, además de Kafka/JWT:

- `kong-server.crt/key`
- `iam-grpc-server.crt/key`
- `notification-grpc-client.crt/key`
- `ca.crt`

## 3. Confiar en la CA local

Abra PowerShell **como Administrador**:

```powershell
.\scripts\install-dev-ca.ps1
```

Equivale a instalar `platform\certs\ca.crt` en el almacén Root de Windows.

## 4. Configurar Google

En Google Cloud → OAuth Client → Authorized redirect URIs:

```text
https://localhost:8443/login/oauth2/code/google
```

No es necesario agregar `GOOGLE_REDIRECT_URI` si se usa la configuración incluida.

## 5. Configurar TikTok/ngrok

```powershell
ngrok http https://localhost:8443
```

Copie el dominio HTTPS generado a `.env`:

```text
TIKTOK_REDIRECT_URI=https://TU-DOMINIO.ngrok-free.app/api/v1/auth/social/tiktok/callback
```

Registre exactamente esa URI en TikTok Developers.

## 6. Levantar el sistema

Antes de crear contenedores, V3.6.5 verifica que los puertos host no estén ocupados por otra versión o proceso.

```powershell
.\scripts\run-local.ps1
```

Para ejecutar únicamente el preflight:

```powershell
.\scripts\preflight-local.ps1
```

O manualmente:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env up -d --build
```

## 7. URLs

```text
Angular       http://localhost:4200
Kong API      https://localhost:8443
Kong Admin    http://localhost:8001
Vault         http://localhost:8200
Grafana       http://localhost:3000
Prometheus    http://localhost:9090
MinIO         http://localhost:9001
```

Abrir `https://localhost:8443` directamente puede responder `no Route matched`; eso es normal si se pide `/`. Use las rutas API o Angular.

## 8. Verificar observabilidad y endpoints

```powershell
.\scripts\verify-local-stack.ps1
```

Comprueba MinIO, Vault, Kong Admin, Angular, Prometheus, Grafana, Loki, Tempo, OpenTelemetry Collector y Alloy.

## 9. Ver credenciales locales

```powershell
.\scripts\show-local-credentials.ps1
```

## 10. Validar HTTPS/OAuth2/gRPC

Con los contenedores arriba:

```powershell
.\scripts\verify-v36-security.ps1
```

Comprueba Compose, HTTPS de Kong, OAuth2 Client Credentials y evidencia de inicio del servidor IAM gRPC mTLS.

## 11. Detener sin borrar datos

```powershell
.\scripts\stop-local.ps1
```

No usa `-v`; los volúmenes se conservan.
