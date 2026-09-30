# Validación de la entrega V3.3

La entrega incluye validación estática de estructura, XML Maven, YAML/JSON, scripts Bash, contratos y consistencia de referencias entre Docker Compose/Kong/Prometheus/Kafka.

Se agregó `query-service`, `query_db`, Redis, Redis Exporter, proyecciones CQRS, ACL Kafka del nuevo principal y rutas del frontend hacia el Query Side.

El entorno de generación no dispone de Maven ni Docker CLI, por lo que no se afirma ejecución completa de `mvn test` ni `docker compose up`. Node/TypeScript está disponible sin dependencias Angular instaladas; se realiza validación sintáctica de TypeScript cuando es posible sin resolver módulos.

Comandos recomendados en un equipo con Docker/Maven:

```bash
mvn clean test
./scripts/generate-dev-certs.sh
docker compose config
docker compose up --build
./scripts/verify-kafka-security.sh
./scripts/rebuild-query-projections.sh
```
