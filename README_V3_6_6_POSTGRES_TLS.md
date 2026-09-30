# Motor Scoring Distribuido V3.6.6 - PostgreSQL TLS obligatorio

V3.6.6 parte de V3.6.5 y agrega cifrado TLS verificado a todas las conexiones TCP hacia PostgreSQL, sin retirar JWT en Kong, SASL_SSL en Kafka, Vault AppRole ni las validaciones de Spring Security.

## Que cambia

PostgreSQL arranca con `ssl=on` usando el certificado `postgres-server.crt` firmado por la CA de desarrollo del proyecto. El certificado contiene SAN para `postgres`, `localhost` y `127.0.0.1`.

Todas las conexiones JDBC de los ocho microservicios usan:

```text
sslmode=verify-full
sslrootcert=/run/secrets/ca.crt
```

Ejemplo:

```text
jdbc:postgresql://postgres:5432/payment_db?sslmode=verify-full&sslrootcert=/run/secrets/ca.crt
```

`verify-full` cifra la sesion, valida que el certificado este firmado por la CA confiable y comprueba que el hostname `postgres` coincida con el certificado.

## TLS obligatorio

`platform/postgres/pg_hba_tls.conf` permite conexiones TCP solamente mediante `hostssl` + `scram-sha-256` y rechaza explicitamente `hostnossl`. Las conexiones locales Unix socket siguen disponibles para la inicializacion de la imagen oficial.

La configuracion efectiva es:

```text
local    all  all                       trust
hostssl  all  all  0.0.0.0/0           scram-sha-256
hostssl  all  all  ::/0                scram-sha-256
hostnossl all all  0.0.0.0/0           reject
hostnossl all all  ::/0                reject
```

## Certificados y permisos

El contenedor PostgreSQL recibe `platform/certs` como solo lectura. `start-postgres-tls.sh` copia `postgres-server.crt`, `postgres-server.key` y `ca.crt` a un directorio Linux interno antes de arrancar PostgreSQL. Esto permite aplicar `0600` a la clave privada incluso en Docker Desktop/Windows.

No se usa la clave privada de la CA para ejecutar PostgreSQL.

## Bases independientes

Se mantiene un servidor PostgreSQL local con bases y usuarios separados:

- `iam_db` / `iam_user`
- `credit_db` / `credit_user`
- `payment_db` / `payment_user`
- `scoring_db` / `scoring_user`
- `report_db` / `report_user`
- `notification_db` / `notification_user`
- `audit_db` / `audit_user`
- `query_db` / `query_user`

Las contrasenas continúan naciendo en `secrets/bootstrap.env`, se cargan en Vault por servicio y los microservicios las consumen desde su namespace correspondiente.

## Verificacion automatica

Ejecute:

```powershell
.\scripts\verify-postgres-tls.ps1
```

Valida tres condiciones:

1. PostgreSQL reporta `ssl=on`.
2. Una conexion `verify-full` negocia TLS y puede verse en `pg_stat_ssl`.
3. Una conexion TCP con `sslmode=disable` es rechazada.

`verify-local-stack.ps1` ejecuta esta prueba automaticamente antes de la comprobacion JWT de Kong.

## DBeaver

Para Payment, por ejemplo:

```text
Host: localhost
Port: 5432
Database: payment_db
Username: payment_user
Password: PAYMENT_DB_PASSWORD
SSL mode: verify-full
Root certificate / CA: platform/certs/ca.crt
```

El certificado del servidor incluye `localhost`, por lo que la verificacion de hostname funciona desde el host.

## Migracion desde V3.6.5

Puede copiar `.env`, `secrets/bootstrap.env` y `platform/certs` desde V3.6.5. Los certificados generados por V3.6.5 ya incluyen `postgres-server.crt` y `postgres-server.key` si fueron creados con los scripts incluidos en esa version.

No regenere certificados si desea conservar las claves actuales. Si faltan los archivos de PostgreSQL, `run-local.ps1` detectara la ausencia y ejecutara el generador de desarrollo completo.

## Capas principales de seguridad resultantes

```text
Angular -> Kong          : HTTPS/TLS + JWT RS256 + CORS + rate limiting
Kong -> microservicios   : HTTP interno + JWT reenviado
Microservicios -> Kafka  : SASL_SSL + SCRAM-SHA-512 + ACL
Microservicios -> Postgres: TLS verify-full + usuario/password
Notification -> IAM gRPC : mTLS + OAuth2 Client Credentials
Microservicios -> Vault  : AppRole (HTTP local en esta version)
```

Como alternativa a copiar manualmente los archivos en Windows:

```powershell
.\scripts\migrate-v365-local-config.ps1 -SourceRoot 'D:\motor-scoring-distributed-v3-6-5'
```

Este script copia configuración y certificados, pero **no** copia los volúmenes Docker de V3.6.5.
