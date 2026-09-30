# V3.6.8 — Seguimiento secuencial del workflow

Esta versión parte de V3.6.7 estable y conserva Kong JWT, HTTPS interno con validación de CA, PostgreSQL TLS verify-full, Kafka SASL_SSL, Vault y observabilidad.

## Cambio funcional

El Paso 3 de Angular ahora muestra el workflow en seis filas secuenciales:

1. Pago aprobado
2. Solicitud enviada
3. Evaluación de riesgo / scoring
4. Generación de reporte PDF
5. Envío de correo
6. Completado

Estados visuales:

- PENDING: gris.
- IN_PROGRESS: amarillo con spinner.
- SUCCESS: fila verde con check grande.
- ERROR: fila roja con X grande y mensaje del backend.

Si el backend avanza varias etapas entre dos polls, Angular revela los éxitos uno por uno para evitar que todos los checks aparezcan de golpe. La fuente de verdad sigue siendo el estado recibido del Query Service.

## Query Service

Se añade `failureStage` y `failureReason` al read model del usuario para identificar exactamente si el fallo corresponde a PAYMENT, SUBMITTED, SCORING, REPORT o EMAIL. Flyway incorpora `V2__workflow_failure_details.sql`.

## Polling

Durante el Paso 3 se consulta el estado cada 1.2 segundos. Aunque una operación termine muy rápido, la UI conserva una transición secuencial corta entre etapas.

## Migración local

Desde V3.6.7:

```powershell
.\scripts\migrate-v367-local-config.ps1 -SourceRoot "D:\motor-scoring-distributed-v3-6-7"
```

Luego:

```powershell
Set-ExecutionPolicy -Scope Process Bypass -Force
.\scripts\run-local.ps1
```
