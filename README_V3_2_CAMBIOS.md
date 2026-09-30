# V3.2 - Kafka seguro e IAM social preservado

Esta versión parte de V3.1 y activa en el entorno distribuido la seguridad Kafka que existía como referencia en el monolito inicial.

## Cambios principales

- Kafka KRaft con **TLS/SSL** en listeners interno y externo.
- Autenticación **SASL/SCRAM-SHA-512** para cada cliente.
- **ACL de mínimo privilegio por microservicio, topic y consumer group**.
- Principal separado para Schema Registry.
- Todos los servicios Spring usan `SASL_SSL`, SCRAM y `client.truststore.jks`.
- `kafka-init` crea topics y ACL antes de iniciar los microservicios.
- Se mantienen Outbox, at-least-once, consumidores idempotentes, retry y DLT.
- Se preservan los flujos IAM heredados: login local, MFA, JWT, Google OAuth2/OIDC, TikTok OAuth2 y Gmail SMTP.
- Se restauran en Docker Compose las variables de configuración Google/TikTok/Gmail del IAM.
- Se mantienen observabilidad V3.1 y módulo `/admin`.

## Importante

Los certificados generados por `scripts/generate-dev-certs.sh` y las contraseñas de `.env.example` son exclusivamente de laboratorio. En producción deben provenir de un Secret Manager/Vault y de una PKI administrada.
