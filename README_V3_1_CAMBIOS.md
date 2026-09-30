# V3.1 - Observabilidad, Auditoría y Administración

## Objetivo
Completar la observabilidad técnica de la V3 y agregar un módulo administrativo mínimo de soporte/auditoría sin alterar el dominio puro de scoring ni la Saga por coreografía.

## Trazabilidad
- No existe reevaluación: una solicitud tiene un único `requestId` y un único `correlationId` de workflow.
- `Credit Service` es propietario de esa relación.
- Los eventos conservan `eventId`, `correlationId`, `causationId`, `traceId` y `aggregateId` (`requestId`).
- Los logs incluyen `requestId`, `correlationId`, `eventId` y `traceId` cuando están disponibles.

## Observabilidad técnica
- OpenTelemetry Java Agent en los 7 microservicios.
- Spans explícitos para operaciones relevantes (`process-payment`, `calculate-scoring`, `generate-report`, `send-notification`, etc.).
- OTLP -> OpenTelemetry Collector -> Tempo.
- Actuator + Micrometer + Prometheus registry -> Prometheus.
- Logs stdout -> Grafana Alloy -> Loki.
- Grafana provisiona Prometheus, Loki y Tempo y un dashboard inicial.

**Observabilidad = entender técnicamente cómo se comportó el sistema.**

- Prometheus: ¿fue un caso aislado o un problema general?
- Loki: ¿qué error concreto ocurrió?
- Tempo: ¿dónde ocurrió y cuánto demoró?
- Grafana: visualiza métricas, logs y trazas.

## Auditoría funcional
`Audit Service` consume eventos Kafka y persiste evidencia de negocio en `audit_db`. También indexa mensajes `.DLT` en `dlt_event`.

**Auditoría = reconstruir funcionalmente qué ocurrió en una operación de negocio.**

Endpoints ADMIN:
- `GET /api/v1/admin/operations`
- `GET /api/v1/admin/operations/{requestId}`
- `GET /api/v1/admin/audit`
- `GET /api/v1/admin/dlt`

## Módulo administrativo
Ruta Angular: `/admin` con `ROLE_ADMIN`.

Secciones:
1. **Operaciones**: resumen por `requestId`, estado, `correlationId`, score, recomendación y timeline.
2. **Auditoría**: eventos persistentes, `eventId`, `causationId`, `traceId`, servicio y fecha.
3. **Errores / DLT**: eventos que agotaron reintentos y terminaron en Dead Letter Topic.

Desde el detalle de Operaciones se enlaza a Grafana para logs (Loki), trazas (Tempo) y métricas (Prometheus).

## Usuario administrador de demo
En una base limpia, IAM provisiona un usuario ADMIN configurable por variables de entorno:
- `IAM_ADMIN_EMAIL` (default local: `admin@finanscore.local`)
- `IAM_ADMIN_PASSWORD` (V3.5: generado localmente; sin valor fijo)
- `IAM_ADMIN_DISPLAY_NAME`

El administrador debe completar MFA en su primer acceso. Las credenciales por defecto son solo para demo local y deben sustituirse en cualquier entorno compartido.
