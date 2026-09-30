# Contratos distribuidos

## Eventos Kafka

Los eventos usan un envelope común definido en `contracts-java`:

- `eventId`
- `eventType`
- `eventVersion`
- `occurredAt`
- `correlationId`
- `causationId`
- `traceId`
- `source`
- `aggregateId`
- `payload`

Los JSON Schema de referencia están en `contracts/event-schemas/`. El Docker Compose incluye Schema Registry para la evolución futura de contratos. La serialización actual del laboratorio es JSON; el registro/validación automática de schemas queda como hardening de contrato.

## Contrato síncrono

`contracts/protobuf/iam_user_directory.proto` define la consulta puntual de identidad que Notification Service necesitará para obtener el correo registrado. Esta versión conserva un adaptador REST temporal para facilitar el laboratorio, pero el puerto de aplicación evita acoplar el caso de uso a REST.

## Reglas de evolución

1. Nunca reutilizar un significado anterior con un payload incompatible.
2. Incrementar `eventVersion` cuando cambie el contrato.
3. Añadir campos opcionales antes que eliminar/renombrar campos existentes.
4. Mantener consumidores compatibles durante una ventana de migración.
5. Kafka transporta referencias de archivos, no PDFs binarios.

## Evento V3.2
`CreditRequestCreated` (`credit.request.created.v1`) se publica al registrar la solicitud. Permite que Audit Service conozca la operación desde `AWAITING_PAYMENT` sin consultar directamente `credit_db`.


## Consumo CQRS V3.3

Query Service consume los contratos de eventos existentes; no introduce un evento de negocio nuevo. Las proyecciones son consumidores derivados y no cambian el contrato del Command Side.
