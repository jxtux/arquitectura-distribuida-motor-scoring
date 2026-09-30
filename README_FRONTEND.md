# Frontend Angular

## Navegación privada

- Nueva evaluación
- Mis solicitudes
- Perfil/IAM existente
- Cerrar sesión

Existe un módulo administrativo separado y protegido con `ROLE_ADMIN`.

## Stepper

### 1 — Solicitud
IAM aporta identidad y correo. El usuario ingresa únicamente producto, monto, plazo, finalidad y consentimiento. Los datos financieros se obtienen del proveedor crediticio del backend.

### 2 — Pago
Concepto en select (`Evaluación crediticia` por defecto), moneda PEN/USD/EUR/CNY y cotización calculada por Payment Service. Tarjetas demo: Visa, Mastercard, American Express y Diners Club. Tras `Realizar pago` aparece un modal de confirmación.

Si cancela no se envía nada. Si confirma, se envía `Idempotency-Key`. Un número compuesto solo por ceros produce `Pago no válido`; un formato ficticio válido devuelve `APPROVED` en el simulador.

### 3 — Estado
Polling ligero consulta el **Query Service** (Read Model CQRS). Se muestra: pago, solicitud enviada, scoring, reporte, notificación y completado/fallido. La pantalla informa que puede cerrarse; Kafka/backend continúan.

## Mis solicitudes

Muestra solo las solicitudes del JWT actual: ID, fecha, estado amigable, score/recomendación cuando existan y botón para descargar PDF cuando Report Service lo publique.

## Módulo administrativo V3.3

`/admin` está protegido por `adminGuard` y por `ROLE_ADMIN` en backend. **Operaciones** consulta Query Service; **Auditoría** y **Errores/DLT** consultan Audit Service. Contiene:
- `/admin/operaciones`: resumen y búsqueda por requestId/correlationId/userId.
- `/admin/operaciones/:requestId`: detalle, timeline de auditoría y accesos a Grafana.
- `/admin/auditoria`: evidencia histórica de eventos.
- `/admin/errores-dlt`: mensajes que terminaron en DLT.

El módulo no reemplaza Grafana: Operaciones/Auditoría muestran el estado funcional; Grafana se usa para métricas, logs y trazas técnicas.
