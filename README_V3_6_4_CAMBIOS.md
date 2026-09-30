# Motor de Scoring Distribuido V3.6.4

Versión consolidada de estabilización local y correcciones funcionales/UI.

## Cambios incluidos

- MinIO se construye localmente desde el tag `RELEASE.2025-09-07T16-13-09Z`, evitando la dependencia del registro que devolvía 401.
- Puertos de infraestructura/observabilidad configurables desde `.env` y preflight de puertos antes del arranque.
- Versiones fijadas para Prometheus, Grafana, Loki, Tempo, OTel Collector, Alloy y Redis Exporter (`v1.86.0`).
- Scripts `preflight-local.ps1` y `verify-local-stack.ps1` para validar el entorno y endpoints principales.
- Scoring demo compatible con usuarios OAuth/Google/TikTok sin perfil explícito: asigna de forma determinista uno de los 5 perfiles sintéticos de demo.
- Paso 3 de Angular mejorado: checks grandes azules, etapa actual en amarillo claro y mensaje final de proceso completado.
- Admin Audit/DLT: corrección de mapeo JPA de campos `TEXT` que se trataban como PostgreSQL Large Objects y producían HTTP 500.
- Lecturas administrativas de Audit/DLT con transacción de solo lectura.
- Detalle de operación tolerante a fallos parciales de Audit/DLT para no ocultar el resumen de Query Service.

## Arranque local

Si ya cuentas con `.env` y `secrets/bootstrap.env` de la versión anterior, puedes copiarlos a esta versión, generar/instalar certificados y ejecutar `scripts/run-local.ps1`.

Para detener sin borrar datos:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env down
```

No usar `-v` si deseas conservar los volúmenes.
