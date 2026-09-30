# Motor de Scoring Distribuido V3.6.1 — Hotfix de estabilización

Esta versión parte de V3.6 y consolida los problemas detectados durante la validación local real.

## Correcciones incluidas

1. **Scoring + PostgreSQL**: `scoring-service` arranca con `SPRING_PROFILES_ACTIVE=postgres`, de modo que los adaptadores JPA del perfil PostgreSQL se registran correctamente.
2. **Vault AppRole**: `platform/vault/init-vault.sh` tolera de forma idempotente únicamente el caso `SecretID is already registered`; otros errores siguen deteniendo la inicialización. En un Vault limpio registra los SecretID normalmente.
3. **Object Storage**: se elimina `quay.io/minio/aistor/minio`, porque AIStor exige licencia en operaciones de servicio. Se usa `quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z` (MinIO Community) para la demo S3 local.
4. **Frontend**: si el workflow termina en `FAILED`, Angular muestra una alerta visible y deja de prometer el PDF/correo como si el proceso hubiera finalizado correctamente. Los errores del polling, incluido 401, también son visibles.
5. **Proyecto aislado**: el nombre Compose pasa a `motor-scoring-distributed-v3-6-1`, evitando mezclar volúmenes y contenedores con V3.6.

## Primera ejecución recomendada

Desde la carpeta `motor-scoring-distributed-v3-6-1`:

```powershell
Copy-Item .env.example .env
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\prepare-local-secrets.ps1
# Complete GOOGLE_CLIENT_SECRET / TIKTOK_CLIENT_SECRET / GMAIL_APP_PASSWORD en secrets/bootstrap.env si aplica.
.\scripts\generate-dev-certs.ps1
# PowerShell como administrador, una sola vez por CA local:
.\scripts\install-dev-ca.ps1
.\scripts\run-local.ps1
```

Si desea reutilizar la configuración privada de V3.6, copie manualmente `.env` y `secrets/bootstrap.env` desde su instalación anterior **sin incluirlos en control de versiones**. Los certificados también pueden regenerarse.

## Validación rápida

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env ps -a

docker compose logs --tail=120 iam-service credit-service payment-service scoring-service report-service notification-service audit-service query-service |
  Select-String "Started .*Application|invalid role|permission denied|password authentication failed|APPLICATION FAILED|Application run failed"
```

Para validar MinIO Community:

```powershell
docker compose logs --tail=80 minio
```

No debe aparecer `No license is installed`.

## Nota sobre solicitudes anteriores

Las solicitudes que ya terminaron en DLT/`FAILED` en V3.6 no deben reutilizarse para validar V3.6.1. Cree una solicitud nueva en el nuevo stack para comprobar el flujo completo.

## Observabilidad

Tempo no forma parte de este hotfix funcional. Si `tempo` continúa en `Exited (1)`, el flujo de negocio puede validarse igualmente; su corrección se trata como ajuste separado de observabilidad.
