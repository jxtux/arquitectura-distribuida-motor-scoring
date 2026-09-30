# Administración, auditoría y observabilidad

## Diferencia
- **Operaciones**: vista resumida y orientada a soporte. Responde: "¿cómo está esta solicitud y hasta dónde llegó?".
- **Auditoría**: evidencia detallada e histórica. Responde: "¿qué ocurrió, cuándo, qué servicio lo hizo y qué evento causó al siguiente?".
- **Grafana**: diagnóstico técnico. Consulta Prometheus (métricas), Loki (logs) y Tempo (trazas).

## Flujo de investigación
1. El administrador busca un `requestId` en `/admin/operaciones`.
2. Query Service busca `operation_read_model` por `requestId` y devuelve el estado operativo consolidado.
3. Si se abre el detalle, Audit Service aporta el timeline histórico y DLT de esa misma operación.
4. Si hace falta diagnóstico técnico, Grafana usa `requestId`, `correlationId` o `traceId` como filtro.

## Seguridad
Los endpoints `/api/v1/query/admin/**` (Operaciones) y `/api/v1/admin/**` (Auditoría/DLT) exigen un JWT válido con `ROLE_ADMIN`. La ruta Angular `/admin/**` también usa un guard de rol. El control real está en backend; el guard frontend es solo una mejora de UX.


## CQRS

Operaciones no reconstruye el estado leyendo `audit_db`; desde V3.3 usa la proyección persistente de Query Service. Audit Service conserva exclusivamente la historia/evidencia.
