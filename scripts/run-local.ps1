param(
  [switch]$SkipPortCheck,
  [switch]$SkipVerify
)
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

if (-not (Test-Path '.env')) { Copy-Item '.env.example' '.env' }
if (-not (Test-Path 'secrets\bootstrap.env')) { & .\scripts\prepare-local-secrets.ps1 }
if (-not (Select-String -Path 'secrets\bootstrap.env' -Pattern '^OAUTH2_NOTIFICATION_CLIENT_SECRET=' -Quiet)) {
  throw 'bootstrap.env es de una version anterior. Agregue OAUTH2_NOTIFICATION_CLIENT_SECRET o regenere con .\scripts\prepare-local-secrets.ps1 -Force'
}
if (-not (Test-Path 'platform\certs\client.truststore.jks') -or
    -not (Test-Path 'platform\certs\kong-server.crt') -or
    -not (Test-Path 'platform\certs\iam-grpc-server.crt') -or
    -not (Test-Path 'platform\certs\notification-grpc-client.crt') -or
    -not (Test-Path 'platform\certs\postgres-server.crt') -or
    -not (Test-Path 'platform\certs\postgres-server.key') -or
    -not (Test-Path 'platform\certs\ca.crt') -or
    -not (Test-Path 'platform\certs\iam-public.pem')) {
  & .\scripts\generate-dev-certs.ps1
}

$httpsKeystores = @(
  'iam-service','credit-service','payment-service','scoring-service',
  'report-service','notification-service','audit-service','query-service'
) | ForEach-Object { Join-Path $Root ("platform\certs\$($_)-https.p12") }
if (@($httpsKeystores | Where-Object { -not (Test-Path $_) }).Count -gt 0) {
  & .\scripts\generate-service-https-certs.ps1
}

& .\scripts\render-kong-config.ps1

if (-not $SkipPortCheck) {
  & .\scripts\preflight-local.ps1
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

docker compose --env-file .env --env-file secrets/bootstrap.env config | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'docker compose config fallo.' }

docker compose --env-file .env --env-file secrets/bootstrap.env up --build -d
if ($LASTEXITCODE -ne 0) { throw 'docker compose up fallo.' }

if (-not $SkipVerify) {
  & .\scripts\verify-local-stack.ps1 -WaitSeconds 180
  if ($LASTEXITCODE -ne 0) { throw 'El stack arranco parcialmente; revise los servicios marcados por verify-local-stack.ps1.' }
}
