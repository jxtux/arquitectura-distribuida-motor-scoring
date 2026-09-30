# V3.3 - CQRS formal + Read Models + Redis

V3.3 parte de V3.2 y mantiene IAM, Kafka seguro, Saga, Outbox, auditoría y observabilidad. El cambio principal es separar formalmente el lado de escritura del lado de consulta.

## Nuevo Query Service

Se agrega `query-service` (puerto interno 8088) con `query_db` propia. Consume los eventos del workflow desde Kafka y construye proyecciones optimizadas para lectura:

- `operation_read_model`: vista administrativa de una solicitud.
- `user_request_read_model`: vista de “Mis solicitudes”.
- `processed_projection_event`: idempotencia de la proyección.

`Credit Service` queda como Command Side público para crear solicitudes. Sus antiguos GET públicos de consulta se retiran; Angular consulta el Query Side.

## Redis

Redis se usa como caché opcional del Query Service mediante cache-aside:

1. Query Service intenta leer Redis.
2. En cache miss consulta PostgreSQL `query_db`.
3. Guarda la respuesta con TTL configurable.
4. Cuando una proyección cambia, invalida las claves de esa solicitud/usuario.
5. Si Redis falla, Query Service continúa consultando PostgreSQL.

Se exponen métricas `query_cache_hits_total`, `query_cache_misses_total`, `query_cache_errors_total`, `query_cache_writes_total`, `query_cache_evictions_total` y `query_postgres_latency`. También se agrega `redis-exporter` a Prometheus.

## CQRS

```text
COMMAND SIDE
Angular -> Kong -> Credit / Payment / Scoring / Report / Notification
                        |
                     BD propias
                        |
                 Transactional Outbox
                        |
                       Kafka
                        |
               +--------+---------+
               |                  |
         Query Service       Audit Service
               |                  |
            query_db            audit_db
               |
        Read Models + Redis
               |
       Admin / Mis solicitudes
```

Audit Service no se convierte en Query Service: continúa siendo evidencia histórica. Query Service representa el estado actual optimizado para lectura.

## Seguridad

`query-service` tiene su propio principal Kafka `query-service`, autenticado con TLS + SASL/SCRAM-SHA-512 y ACL de solo lectura sobre los eventos/DLT necesarios. Los endpoints administrativos requieren `ROLE_ADMIN`; los endpoints `my-requests` obtienen `userId` del JWT y no aceptan un usuario arbitrario por parámetro.

## Reconstrucción

`./scripts/rebuild-query-projections.sh` limpia `query_db`/Redis, restablece los offsets de los grupos del Query Service al inicio de la retención disponible y reproduce los eventos Kafka para reconstruir las proyecciones.

V3.3 **no implementa Event Sourcing**: las bases de cada servicio siguen siendo la fuente de verdad del Command Side; `query_db` es una representación derivada y reconstruible.
