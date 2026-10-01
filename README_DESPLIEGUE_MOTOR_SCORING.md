# Motor de Scoring Distribuido — Guía de Despliegue

Versión: **V3.6.8.1**  
Arquitectura: **Microservicios + Clean/Hexagonal + Kafka + Kong + Vault + PostgreSQL + Redis + MinIO + Observabilidad**

## 1. Requisitos previos

- Windows 10/11.
- Docker Desktop.
- WSL 2 habilitado.
- PowerShell.

Comprobar Docker:

```powershell
docker version
```

Comprobar WSL:

```powershell
wsl -l -v
```

## 2. Ubicación del proyecto

```powershell
cd D:\motor-scoring-distributed-v3-6-8-1
```

## 3. Archivos de configuración

El despliegue utiliza principalmente:

```text
.env
secrets/bootstrap.env
docker-compose.yml
```

`secrets/bootstrap.env` contiene credenciales y secretos para PostgreSQL, Kafka, Redis, MinIO, Vault, Grafana, usuarios demo de IAM y OAuth2 Client Credentials.

## 4. Primer despliegue

```powershell
Set-ExecutionPolicy -Scope Process Bypass -Force
.\scripts\run-local.ps1
```

El primer arranque puede tardar varios minutos porque Docker puede descargar imágenes y compilar los microservicios.

## 5. Verificar el despliegue

```powershell
.\scripts\verify-local-stack.ps1
```

La validación comprueba:

- Angular.
- Kong.
- MinIO.
- Vault.
- Kafka.
- PostgreSQL.
- Redis.
- Prometheus, Grafana, Loki, Tempo, OpenTelemetry Collector y Alloy.
- 8/8 microservicios visibles en Prometheus.
- HTTPS entre Kong y microservicios.
- TLS obligatorio en PostgreSQL.
- JWT perimetral en Kong.
- CORS/preflight.

El resultado final debe ser equivalente a:

```text
Validacion OK: endpoints, HTTPS interno, observabilidad,
PostgreSQL TLS y JWT perimetral de Kong responden.
```

## 6. URLs principales

| Componente | URL |
|---|---|
| Angular | http://localhost:4200 |
| Kong Proxy | https://localhost:8443 |
| Kong Admin | http://localhost:8001 |
| MinIO Console | http://localhost:9001 |
| MinIO API | http://localhost:9000 |
| Vault | http://localhost:8200 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Loki | http://localhost:3100 |
| Tempo | http://localhost:3200 |
| OpenTelemetry Collector | http://localhost:13133 |
| Alloy | http://localhost:12345 |
| PostgreSQL | localhost:5432 |

## 7. Flujo funcional de prueba

Abrir:

```text
http://localhost:4200
```

Flujo recomendado:

```text
Login
  ↓
MFA, si corresponde
  ↓
Crear solicitud
  ↓
Realizar pago
  ↓
Scoring
  ↓
Generación de reporte PDF
  ↓
Notificación
  ↓
Mis solicitudes
  ↓
Descarga del PDF
```

El procesamiento principal se realiza de forma asíncrona mediante Kafka y Saga por coreografía.

## 8. Arquitectura de comunicación

### Comunicación externa

```text
Angular
   ↓ HTTPS + JWT
Kong
   ↓ HTTPS + validación TLS
Microservicios
```

Kong realiza routing, TLS, JWT, CORS y rate limiting. El rate limit global configurado es de **120 solicitudes por minuto**.

### Comunicación asíncrona

```text
Credit
  ↓
Kafka
  ↓
Payment
  ↓
Kafka
  ↓
Scoring
  ↓
Kafka
  ↓
Report
  ↓
Kafka
  ↓
Notification
```

Kafka utiliza SASL_SSL, SCRAM-SHA-512, ACL, TLS, Transactional Outbox, consumidores idempotentes y Retry + DLT.

### Comunicación síncrona puntual

```text
Notification Service
        ↓
gRPC + mTLS + OAuth2 Client Credentials
        ↓
IAM Service
```

gRPC se utiliza solo cuando se necesita una respuesta inmediata.

## 9. Persistencia

Se utiliza Database per Service:

```text
iam_db
credit_db
payment_db
scoring_db
report_db
notification_db
audit_db
query_db
```

PostgreSQL exige TLS para conexiones TCP y los microservicios utilizan `sslmode=verify-full`.

## 10. Redis

Obtener la contraseña:

```powershell
$redisPassword = ((Get-Content .\secrets\bootstrap.env | Where-Object { $_ -match '^REDIS_PASSWORD=' }) -replace '^REDIS_PASSWORD=','')
```

Probar:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env exec -e REDISCLI_AUTH="$redisPassword" redis redis-cli PING
```

Resultado esperado:

```text
PONG
```

## 11. MinIO

Abrir:

```text
http://localhost:9001
```

Usuario por defecto:

```text
minioadmin
```

Obtener la contraseña:

```powershell
(Get-Content .\secrets\bootstrap.env | Where-Object { $_ -match '^MINIO_ROOT_PASSWORD=' }) -replace '^MINIO_ROOT_PASSWORD=',''
```

Report Service almacena en MinIO los PDF generados. Kafka transporta la referencia y metadata, no el PDF completo.

## 12. Observabilidad

### Métricas

```text
Spring Boot Actuator → Prometheus → Grafana
```

Consulta de prueba:

```promql
up
```

`1` indica que el target está disponible.

### Logs

```text
Microservicios stdout → Grafana Alloy → Loki → Grafana
```

Ejemplo LogQL:

```logql
{service="query-service"}
```

### Trazas

```text
Microservicio → OpenTelemetry Java Agent → OTel Collector → Tempo → Grafana
```

Ejemplo TraceQL:

```traceql
{ resource.service.name = "query-service" }
```

## 13. Ver contenedores

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env ps
```

Los contenedores siguientes pueden aparecer como `Exited (0)` y es normal:

```text
kafka-init
query-db-init
vault-init
```

`Exited (0)` significa que terminaron correctamente su tarea de inicialización.

## 14. Ver logs

Todos:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env logs -f
```

Kong:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env logs -f kong
```

Query Service:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env logs -f query-service
```

Report Service:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env logs -f report-service
```

Kafka:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env logs -f kafka
```

## 15. Detener sin eliminar datos

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env stop
```

Esto conserva contenedores, redes y volúmenes.

## 16. Volver a iniciar

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env start
```

Después:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env ps
```

Como Vault se ejecuta en modo dev y mantiene información en memoria, si después de reiniciar Docker/WSL algún servicio falla por Vault:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env up -d vault vault-init
docker compose --env-file .env --env-file secrets/bootstrap.env start
```

## 17. Reinicio completo conservando volúmenes

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env down
.\scripts\run-local.ps1
```

`down` elimina contenedores y red, pero conserva los volúmenes.

## 18. No borrar volúmenes

No ejecutar si se desea conservar la información:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env down -v
```

`-v` elimina los volúmenes persistentes y puede borrar datos de PostgreSQL, Kafka, MinIO, Redis y Grafana.

## 19. Prueba rápida de Kong

Ruta protegida sin JWT:

```powershell
curl.exe -k -i https://localhost:8443/api/v1/query/my-requests
```

Resultado esperado:

```text
401 Unauthorized
```

Preflight CORS:

```powershell
curl.exe -k -i -X OPTIONS https://localhost:8443/api/v1/query/my-requests -H "Origin: http://localhost:4200" -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: Authorization,Content-Type"
```

Resultado esperado:

```text
200
```

## 20. Componentes principales

```text
Angular
Kong
IAM Service
Credit Service
Payment Service
Scoring Service
Report Service
Notification Service
Audit Service
Query Service
Kafka
Schema Registry
PostgreSQL
Redis
MinIO
HashiCorp Vault
Prometheus
Grafana
Loki
Tempo
OpenTelemetry Collector
Grafana Alloy
```

## 21. Nota sobre el entorno

Esta configuración corresponde al entorno local/demostrativo del proyecto.

Vault se ejecuta en modo dev, por lo que para producción real deben endurecerse configuraciones de secretos, certificados, rotación de claves, alta disponibilidad, almacenamiento persistente de Vault, escalado y políticas operativas.

## 22. Resumen rápido

Primer arranque:

```powershell
cd D:\motor-scoring-distributed-v3-6-8-1
Set-ExecutionPolicy -Scope Process Bypass -Force
.\scripts\run-local.ps1
.\scripts\verify-local-stack.ps1
```

Detener conservando todo:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env stop
```

Volver a iniciar:

```powershell
docker compose --env-file .env --env-file secrets/bootstrap.env start
```

Abrir aplicación:

```text
http://localhost:4200
```

Verificación final:

```powershell
.\scripts\verify-local-stack.ps1
```
