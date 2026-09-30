# Estrategia de pruebas

La arquitectura está preparada para pruebas por servicio y pruebas distribuidas.

## Unitarias

- dominio puro de Scoring (`CalculadorScoring` y reglas);
- `SimulatedPaymentAdapter`: tarjeta válida, solo ceros, formato inválido;
- cotización de monedas;
- validaciones de Credit Service;
- mapeo de estados del workflow.

## Integración

Usar Spring Boot Test + Testcontainers para PostgreSQL, Kafka y MinIO. Casos prioritarios:

- `PaymentValidated` inicia scoring una sola vez;
- eventos duplicados no duplican efectos (`processed_event`);
- mismo `Idempotency-Key` devuelve el mismo pago;
- Outbox conserva el evento aunque Kafka esté temporalmente no disponible;
- retries terminan en DLT cuando corresponde;
- Report almacena el PDF y publica solo la referencia;
- un usuario no puede leer solicitudes/PDF de otro usuario.

## Resiliencia

Probar timeouts, Circuit Breaker, Bulkhead, retry con backoff y recuperación. También fallos parciales: IAM no disponible, MinIO no disponible, SMTP no disponible y Kafka temporalmente inaccesible.

## End-to-end de demo

1. login + MFA;
2. crear solicitud;
3. tarjeta `0000000000000000` → `Pago no válido`, sin scoring;
4. tarjeta ficticia válida → `PaymentValidated`;
5. scoring → reporte → notificación;
6. cerrar la pantalla durante el proceso y comprobar que backend continúa;
7. volver a “Mis solicitudes” y descargar el PDF.

> Esta entrega no incluye todavía la batería completa automatizada de Testcontainers; el objetivo de este README es fijar los casos de aceptación antes del hardening final.

## V3.2 - pruebas Kafka TLS/SCRAM/ACL

Preparación:

```bash
cp .env.example .env
./scripts/generate-dev-certs.sh
docker compose up --build -d
./scripts/verify-kafka-security.sh
```

Validar además que:

- un servicio arranca con `SASL_SSL` y su principal SCRAM;
- Credit puede leer `payment.validated.v1` pero no obtiene permisos ajenos a su bounded context;
- Scoring consume `credit.evaluation.requested.v1` y puede escribir su DLT;
- Audit puede leer los eventos y DLT, pero no actúa como superusuario;
- Schema Registry conecta con su principal dedicado;
- una credencial incorrecta falla en autenticación y una operación fuera de ACL falla en autorización.

## V3.3 - CQRS / Query Service / Redis

Casos añadidos o previstos:

- proyección idempotente: el mismo `eventId` no se aplica dos veces;
- `CreditRequestCreated` crea `operation_read_model` y `user_request_read_model`;
- eventos antiguos no degradan una operación ya `COMPLETED`;
- `ROLE_ADMIN` para `/api/v1/query/admin/**`;
- `SCORE_READ` + `sub` JWT para `/api/v1/query/my-requests/**`;
- cache hit/miss y fallback a PostgreSQL cuando Redis no responde;
- rebuild de proyecciones mediante replay de Kafka;
- ACL de `query-service` solo para lectura de topics/grupos autorizados.
