# Cambios principales respecto del proyecto original

## Conservado

- Java 21 y Spring Boot.
- Clean/Hexagonal Architecture.
- Dominio de scoring y `CalculadorScoring`.
- IAM propio con Spring Security, JWT y MFA.
- conceptos de Kafka, Transactional Outbox e idempotencia.
- frontend Angular y flujo de autenticación.

## Transformado

- un despliegue backend modular → siete servicios desplegables de forma independiente;
- una base principal → Database per Service;
- consumidores técnicos Payment/Scoring/Delivery → servicios Payment, Scoring, Report y Notification;
- códigos de pago estáticos → `PaymentProcessorPort` + `SimulatedPaymentAdapter`;
- captura manual de datos financieros → `CreditDataProviderPort` + buró simulado;
- PDF/email acoplados → Report Service + MinIO + Notification Service;
- estado local del proceso → Saga por coreografía con proyección en Credit Service;
- endpoints directos → Kong como entrada pública;
- auditoría mezclada con operación → Audit Service desacoplado;
- formulario único → stepper Angular de 3 pasos + “Mis solicitudes”.

## Regla funcional central

El sistema **evalúa el riesgo crediticio de una solicitud de préstamo**. No entrega un score universal independiente ni desembolsa un préstamo. Solo después de `PaymentValidated` se inicia la evaluación, que genera puntaje, factores y una recomendación (`PREAPROBADA`, `REVISION_MANUAL` o `RECHAZADA`).
