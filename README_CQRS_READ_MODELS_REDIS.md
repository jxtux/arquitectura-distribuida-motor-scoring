# CQRS, Read Models y Redis

## Responsabilidades

### Command Side
`Credit`, `Payment`, `Scoring`, `Report` y `Notification` ejecutan cambios de estado sobre sus bases propias. Los cambios de integración salen por Transactional Outbox hacia Kafka.

### Query Side
`Query Service` no ejecuta reglas de negocio ni modifica las bases de los servicios de dominio. Consume eventos y mantiene `query_db` con datos desnormalizados listos para consultar.

### Audit Side
`Audit Service` guarda el historial funcional de eventos. No es la fuente del estado operativo que usa Admin.

## Proyecciones

`operation_read_model` contiene el estado operativo consolidado por `requestId`: correlationId, usuario, producto, monto, pago, estado, score, recomendación, reporte, notificación y último evento.

`user_request_read_model` contiene únicamente los campos necesarios para “Mis solicitudes”.

La proyección es idempotente mediante `processed_projection_event`. Además, el estado solo avanza y no se degrada si un evento anterior llega fuera de orden después de un estado terminal.

## Consistencia

El Query Side es eventualmente consistente. Después de un comando puede existir un intervalo corto antes de que la proyección refleje el nuevo estado. El frontend realiza polling/reintentos para seguimiento.

## Redis cache-aside

Claves principales:

- `operation:{requestId}`
- `my-requests:{userId}`
- `my-request:{userId}:{requestId}`

Redis nunca es fuente de verdad. Si no está disponible, el Query Service usa PostgreSQL. La caché tiene TTL e invalidación por evento.

## Rebuild / replay

El script `scripts/rebuild-query-projections.sh`:

1. detiene Query Service;
2. vacía read models e idempotency guard;
3. limpia Redis;
4. resetea los grupos Kafka `query-service-projections` y `query-service-projections-dlt` a los offsets más antiguos aún retenidos;
5. inicia Query Service para reproducir eventos.

La reconstrucción solo puede recuperar eventos que Kafka todavía conserve. Para producción debe definirse una política de retención compatible con la estrategia de rebuild o un archivo histórico de eventos.
