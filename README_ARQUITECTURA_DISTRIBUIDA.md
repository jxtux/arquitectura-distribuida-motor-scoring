# Arquitectura distribuida

## Decisión

El sistema se define como **arquitectura distribuida basada en servicios autónomos y eventos**. Microservicios es el patrón de descomposición; Kafka es el backbone asíncrono.

## Principios

- Database per Service; no FK ni SELECT entre bases de otros servicios.
- Clean/Hexagonal dentro de cada servicio. El dominio de scoring sigue puro.
- Contratos explícitos: JSON Schema para eventos y Protobuf para gRPC.
- Consistencia eventual y Saga por coreografía; no 2PC.
- Transactional Outbox por productor e Idempotent Consumer por consumidor.
- At-least-once en Kafka; los consumidores deben tolerar duplicados.
- gRPC reservado a consultas síncronas puntuales; no coordina la Saga.
- CQRS: los servicios de dominio constituyen el Command Side; Query Service materializa Read Models a partir de Kafka.
- Redis acelera lecturas del Query Side, pero no es fuente de verdad.

## Flujo

1. Credit crea solicitud `AWAITING_PAYMENT`.
2. Payment valida tarjeta ficticia y publica `PaymentValidated` o `PaymentRejected`.
3. Credit consume `PaymentValidated`, cambia a `SUBMITTED` y publica `CreditEvaluationRequested`.
4. Scoring obtiene perfil financiero sintético, reutiliza `CalculadorScoring` y publica `ScoringCalculated`.
5. Report genera el PDF, lo guarda en MinIO y publica `ReportGenerated` con referencia, nunca con bytes del PDF.
6. Notification obtiene el correo del IAM, notifica y publica `NotificationSent`.
7. Query Service consume los eventos y actualiza `operation_read_model` y `user_request_read_model` en `query_db`; Redis acelera lecturas.
8. Admin Operaciones, seguimiento y “Mis solicitudes” leen desde Query Service.
9. Audit consume los eventos en paralelo y conserva la historia funcional sin participar del camino crítico.

## CAP/PACELC

No existe una “librería CAP”. Se documentan decisiones por operación. Credit/Payment priorizan consistencia de estados e idempotencia. Report/Notification/Audit aceptan consistencia eventual. PACELC documenta el compromiso entre latencia y consistencia cuando no hay partición.
