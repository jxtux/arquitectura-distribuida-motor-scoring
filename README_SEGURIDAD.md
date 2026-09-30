# Seguridad — V3.6

## Entrada externa

Kong es el API Gateway. En V3.6 el proxy público local escucha por TLS en `https://localhost:8443`. Angular consume exclusivamente esa URL para las APIs. Kong mantiene routing, CORS y rate limiting; los microservicios validan JWT/roles/scopes donde corresponde.

## OAuth social

Google OAuth2/OIDC funciona detrás de Kong HTTPS. IAM habilita forwarded headers para construir el callback externo correcto. TikTok usa callback HTTPS público mediante ngrok hacia Kong.

## Comunicación interna síncrona

La llamada Notification → IAM User Directory usa:

1. OAuth2 Client Credentials para obtener JWT corto con `scope=iam.user.read`.
2. gRPC sobre TLS mutuo.
3. Certificado cliente `notification-service` firmado por la CA local.
4. Validación en IAM de firma/expiración/issuer/audience/scope/token_use.
5. Coincidencia entre CN mTLS y `sub` OAuth2.

El antiguo `X-Internal-Token` fue eliminado.

## Kafka

Kafka usa TLS, SASL/SCRAM-SHA-512 y ACL por principal/topic/group. Los servicios reciben contraseñas desde su contexto Vault.

## Vault

Cada servicio usa AppRole y solo lee su path KV v2. El secreto OAuth2 de Notification se comparte únicamente con IAM (registro/validación del cliente) y Notification (credencial del cliente). El root token de Vault no se distribuye a los microservicios.

## Certificados locales

`generate-dev-certs.ps1/.sh` genera una CA de desarrollo, certificados Kong y gRPC, truststores Kafka y claves JWT IAM. Son exclusivamente de laboratorio; no deben versionarse ni reutilizarse en producción.
