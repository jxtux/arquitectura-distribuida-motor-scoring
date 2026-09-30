# Datos y persistencia

La versión distribuida aplica **Database per Service**. Las bases pueden convivir en el mismo clúster PostgreSQL para la demo, pero cada servicio tiene su propia base, usuario y esquema lógico. No se permiten FK ni `SELECT` directos entre bases de otros servicios.

| Base | Dueño | Información principal |
|---|---|---|
| `iam_db` | IAM Service | usuarios, credenciales, MFA, roles, permisos, sesiones |
| `credit_db` | Credit Service | solicitudes y estado transaccional del workflow (Command Side) |
| `payment_db` | Payment Service | transacciones simuladas, moneda, importe, marca, last4, estado e idempotency key |
| `scoring_db` | Scoring Service | modelo de scoring reutilizado, perfiles crediticios sintéticos y snapshot de entradas |
| `report_db` | Report Service | metadata y referencia del PDF |
| `notification_db` | Notification Service | estado de notificaciones y destinatario |
| `audit_db` | Audit Service | eventos de auditoría desacoplados |
| `query_db` | Query Service | read models/proyecciones CQRS derivadas de Kafka |

## Datos financieros de demo

Los ingresos, gastos, obligaciones, historial, mora, estabilidad y demás variables necesarias para el scoring **no las ingresa el usuario**. `Scoring Service` consume `CreditDataProviderPort`; la implementación de esta entrega es `SimulatedCreditBureauAdapter` y lee perfiles sintéticos desde `scoring_db`.

Se incluyen cinco perfiles demo asociados por `user_id` a los cinco usuarios ficticios del IAM. No hay FK entre `iam_db` y `scoring_db`; el ID es parte del contrato distribuido.

Además se persiste `evaluation_input_snapshot` con la información usada en cada evaluación. Esto permite reproducibilidad y auditoría sin depender de que el perfil de demo cambie posteriormente.

## Tarjetas y datos sensibles

No existe una tabla de tarjetas válidas. `PaymentProcessorPort` delega en `SimulatedPaymentAdapter`. El servicio persiste solo los datos necesarios para trazabilidad: marca, últimos cuatro dígitos, importes, moneda, estado, idempotency key y timestamps. **PAN completo y CVV no se almacenan.**

## PDFs

El archivo PDF no se guarda en PostgreSQL ni se publica como bytes en Kafka. `Report Service` lo almacena en MinIO; `report_db` conserva su metadata y `objectKey`. Los eventos llevan únicamente la referencia.


## CQRS y Redis

`query_db` no es fuente de verdad. Contiene `operation_read_model`, `user_request_read_model` y el guard idempotente de proyecciones. Puede reconstruirse reproduciendo los eventos Kafka retenidos.

Redis mantiene únicamente caché de lectura con TTL. Si Redis falla, Query Service consulta PostgreSQL; no se pierden datos de negocio.
