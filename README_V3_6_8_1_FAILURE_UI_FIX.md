# V3.6.8.1 — Fix visual de fallos por etapa

Esta versión parte de V3.6.8 y corrige el caso en que el workflow quedaba con estado general `FAILED`, pero Angular no marcaba claramente la etapa fallida.

Cambios:
- El estado `FAILED` tiene prioridad sobre estados visuales anteriores.
- Las etapas anteriores a la falla quedan en verde.
- La etapa exacta de falla queda en rojo con una X grande, título subrayado y `failureReason` visible.
- Las etapas posteriores permanecen pendientes/grises.
- Angular normaliza aliases de `failureStage` (`REPORT`, `PDF`, `REPORT_GENERATING`, etc.).
- Si falta `failureStage`, Angular intenta inferirlo de `failureReason` y del contexto del workflow.
- Query Service corrige la inferencia de DLT: prioriza el estado previo del workflow. Por ejemplo, si `report-service` falla procesando `ScoringCalculated` cuando el read model ya estaba en `REPORT_GENERATING`, la falla se clasifica como `REPORT`, no como `SCORING`.

Ejemplo esperado al detener MinIO durante la generación del reporte:

```text
Pago aprobado                 ✓ verde
Solicitud enviada             ✓ verde
Evaluación de riesgo/scoring  ✓ verde
Generación de reporte PDF     ✕ rojo
Envío de correo               pendiente
Completado                    pendiente
```
