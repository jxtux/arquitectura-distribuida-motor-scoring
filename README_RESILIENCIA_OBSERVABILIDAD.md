# Resiliencia y observabilidad - V3.3

## Resiliencia
- Kafka: entrega at-least-once, consumidores idempotentes, retry con backoff y DLT.
- Productores: Transactional Outbox local.
- Llamadas síncronas puntuales: Resilience4j (timeout/retry/backoff+jitter/circuit breaker/bulkhead donde aplica).

## Tres pilares de observabilidad
### Métricas
Cada microservicio usa Spring Boot Actuator + Micrometer + Prometheus Registry. Prometheus hace pull cada 15 s a `/actuator/prometheus` y centraliza las métricas.

### Logs
Cada microservicio escribe logs enriquecidos en stdout. Grafana Alloy descubre los contenedores Docker y envía sus logs a Loki. Se incluyen campos de correlación cuando el contexto los conoce: `requestId`, `correlationId`, `eventId`, `traceId`.

### Trazas
Cada contenedor Java ejecuta OpenTelemetry Java Agent. La instrumentación automática y los `@WithSpan` relevantes generan spans. Las trazas se exportan por OTLP al OTel Collector y éste las envía a Tempo.

## Visualización
Grafana tiene datasources provisionados para:
- Prometheus = métricas.
- Loki = logs.
- Tempo = trazas.

Se incluye un dashboard inicial `Motor Scoring - Overview`.

## Auditoría no es observabilidad
Audit Service permanece separado. Kafka -> Audit Service -> `audit_db` registra evidencia funcional persistente. La observabilidad puede rotar logs/trazas; la auditoría conserva la historia de negocio definida por la política de datos.

### CorrelationId vs traceId
`correlationId` permanece estable durante todo el workflow de negocio. Debido al Outbox y a los límites asíncronos de Kafka, una misma operación puede producir varios `traceId`; por eso soporte parte de `requestId/correlationId` y usa `traceId` para profundizar en una ejecución técnica concreta.


## Query Service y Redis

Query Service usa el mismo OpenTelemetry Java Agent, logs estructurados, Actuator/Micrometer y scrape de Prometheus que el resto de microservicios. Expone métricas de hit/miss/error/eviction de caché y latencia de PostgreSQL. `redis-exporter` permite observar métricas del servidor Redis desde Prometheus/Grafana. Redis es degradable: un fallo de caché no impide consultar `query_db`.
