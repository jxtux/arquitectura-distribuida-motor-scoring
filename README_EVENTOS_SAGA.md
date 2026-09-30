# Eventos, Saga, Outbox e idempotencia

## Topics

- `payment.validated.v1`
- `payment.rejected.v1`
- `credit.evaluation.requested.v1`
- `scoring.calculated.v1`
- `scoring.failed.v1`
- `report.generated.v1`
- `report.failed.v1`
- `notification.sent.v1`
- `notification.failed.v1`
- `iam.audit.v1`

Cada topic dispone de `.DLT` en el laboratorio.

## Envelope

`eventId`, `eventType`, `eventVersion`, `occurredAt`, `correlationId`, `causationId`, `traceId`, `source`, `aggregateId`, `payload`.

## Saga por coreografía

No hay orquestador central. Cada servicio reacciona al evento anterior y emite el siguiente. Credit Service mantiene la proyección amigable del estado para el usuario.

## Outbox

Los productores escriben negocio + `outbox_event` en la misma base/transacción. Un publisher local envía pendientes a Kafka. La entrega es at-least-once.

## Idempotent Consumer

Cada consumidor utiliza `processed_event` con `(eventId, consumerName)` único para evitar aplicar dos veces un evento duplicado.

## Pago

El endpoint exige `Idempotency-Key`. La combinación `(user_id, idempotency_key)` es única. Un retry devuelve el mismo pago en lugar de cobrar/procesar otra vez.
