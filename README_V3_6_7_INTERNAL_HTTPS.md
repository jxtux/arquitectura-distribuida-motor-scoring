# Motor de Scoring Distribuido V3.6.7 — HTTPS interno Kong → microservicios

V3.6.7 parte de V3.6.6 y conserva JWT perimetral en Kong, PostgreSQL TLS `verify-full`, Kafka `SASL_SSL`, Vault AppRole, gRPC mTLS Notification → IAM y observabilidad.

## Cambio principal

La interfaz HTTP de los ocho microservicios Spring Boot ahora usa HTTPS. Los seis servicios publicados por Kong son consumidos como upstreams HTTPS y Kong valida el certificado del servidor contra la CA interna del proyecto.

```text
Angular --HTTPS--> Kong --HTTPS + cert verify--> IAM/Credit/Payment/Report/Query/Audit
                              |
                              +--> los microservicios vuelven a validar JWT/autorizacion
```

Scoring y Notification también exponen su endpoint HTTP/Actuator con HTTPS aunque no tengan rutas públicas en Kong. Prometheus los scrapea por HTTPS validando la misma CA.

## Certificados

Cada servicio tiene su propio certificado/PKCS12 con SAN correspondiente al DNS de Docker:

- `iam-service-https.p12`
- `credit-service-https.p12`
- `payment-service-https.p12`
- `scoring-service-https.p12`
- `report-service-https.p12`
- `notification-service-https.p12`
- `audit-service-https.p12`
- `query-service-https.p12`

Son firmados por `platform/certs/ca.crt`. Para migrar desde V3.6.6 se conserva la CA existente y se generan únicamente los nuevos certificados de microservicios.

## Spring Boot

Docker Compose configura en cada servicio:

```text
SERVER_SSL_ENABLED=true
SERVER_SSL_KEY_STORE=file:/run/secrets/<service>-https.p12
SERVER_SSL_KEY_STORE_TYPE=PKCS12
SERVER_SSL_KEY_STORE_PASSWORD=<DEV_CERT_PASSWORD>
SERVER_SSL_ENABLED_PROTOCOLS=TLSv1.2,TLSv1.3
```

Los puertos no cambian: IAM 8081, Credit 8082, Payment 8083, Scoring 8084, Report 8085, Notification 8086, Audit 8087 y Query 8088. Lo que cambia es el protocolo: ahora son HTTPS.

## Kong

Los upstreams pasan de `http://` a `https://`. Ejemplo:

```yaml
- name: payment-service
  url: https://payment-service:8083
  tls_verify: true
  tls_verify_depth: 2
  ca_certificates:
  - 7f367000-0000-4000-8000-000000000001
```

`render-kong-config.ps1` inserta tanto la clave pública RSA del IAM para JWT como `ca.crt` para validar los certificados HTTPS internos.

Las rutas siguen con `strip_path: false`, por lo que Kong conserva el path enviado por Angular.

## Prometheus

Prometheus scrapea `/actuator/prometheus` usando:

```yaml
scheme: https
tls_config:
  ca_file: /run/secrets/ca.crt
```

Esto también sirve como comprobación independiente de que los ocho certificados internos son válidos para sus hostnames Docker.

## Migración desde V3.6.6

Desde V3.6.7:

```powershell
.\scripts\migrate-v366-local-config.ps1 -SourceRoot "D:\motor-scoring-distributed-v3-6-6"
.\scripts\run-local.ps1
```

El script copia `.env`, `secrets/bootstrap.env` y `platform/certs`, conserva la CA/JWT existentes y agrega los nuevos certificados HTTPS internos.

## Verificación

```powershell
.\scripts\verify-local-stack.ps1
.\scripts\verify-internal-https.ps1
.\scripts\verify-postgres-tls.ps1
.\scripts\verify-kong-jwt.ps1
```

`verify-internal-https.ps1` comprueba que los seis upstreams de Kong usan `https`, `tls_verify=true` y CA configurada, y que Prometheus reporta `up=1` para los ocho microservicios mediante HTTPS.

## Alcance de seguridad

V3.6.7 implementa TLS de servidor entre Kong y los upstreams. No convierte ese enlace en mTLS: los microservicios validan que hablan con un Kong que llega por la red interna y mantienen JWT/Spring Security como control de aplicación, pero no exigen un certificado cliente de Kong. El mTLS existente Notification → IAM gRPC se conserva sin cambios.
