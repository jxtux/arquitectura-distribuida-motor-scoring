# Motor Scoring Distribuido V3.6.5 - Kong JWT Defense in Depth

Esta version parte de V3.6.4 y agrega autenticacion JWT perimetral en Kong sin eliminar Spring Security de los microservicios.

## Responsabilidades

Kong valida en las rutas protegidas:
- presencia del JWT en `Authorization: Bearer ...`
- firma RS256 con la clave publica de IAM
- claim `exp`
- identifica la credencial mediante `iss=finanscore`

Spring Security vuelve a validar:
- firma RSA y expiracion
- issuer
- audience (`motor-scoring-api`)
- `token_use=ACCESS`
- `mfa=true`
- roles y permissions
- autorizacion de negocio (`ROLE_ADMIN`, `SCORE_READ`, ownership, etc.)

La doble validacion es intencional: Kong protege el perimetro y cada microservicio conserva autoridad sobre su seguridad de negocio.

## Rutas publicas sin JWT de Kong

- `/api/v1/auth/**` (login, registro, MFA, refresh, callbacks sociales)
- `/oauth2/**`
- `/login/oauth2/**`

Estas rutas necesitan funcionar antes de que exista un Access JWT.

## Rutas protegidas por Kong JWT

- `/api/v1/users/**`
- `/api/v1/credit-requests/**`
- `/api/v1/payments/**`
- `/api/v1/reports/**`
- `/api/v1/query/**`
- `/api/v1/admin/**`

## Clave RSA en modo DB-less

La clave privada IAM nunca se entrega a Kong. Kong solo necesita `platform/certs/iam-public.pem`.

`run-local.ps1` ejecuta `render-kong-config.ps1`, que inserta esa clave publica en el `jwt_secret` RS256 del consumer `finanscore-iam` dentro de `gateway/kong/kong.yml`.

El `key` de la credencial JWT es `finanscore`, que coincide con el claim `iss` emitido por IAM.

## CORS

El plugin JWT tiene `run_on_preflight: false`, por lo que las peticiones `OPTIONS` del navegador no necesitan JWT.

## Verificacion

Despues de levantar:

```powershell
.\scripts\verify-kong-jwt.ps1
```

Debe comprobar:
- seis plugins JWT activos
- consumer `finanscore-iam`
- ruta protegida sin token => HTTP 401
- preflight OPTIONS => 200/204

Opcionalmente, desde DevTools puede copiar temporalmente un Access JWT para una prueba local:

```powershell
.\scripts\verify-kong-jwt.ps1 -AccessToken 'eyJ...'
```

No comparta ni persista ese token.

## Inspeccion con Admin API

```powershell
curl.exe http://localhost:8001/consumers
curl.exe http://localhost:8001/plugins
curl.exe http://localhost:8001/routes
```

La Admin API sigue siendo local de desarrollo y no debe exponerse publicamente en produccion.
