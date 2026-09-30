$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root
$Secrets = 'secrets\bootstrap.env'
if (-not (Test-Path $Secrets)) { throw 'Falta secrets\bootstrap.env.' }

Write-Host '1) Validando Docker Compose...' -ForegroundColor Cyan
$config = docker compose --env-file .env --env-file $Secrets config 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) { throw $config }
foreach($required in @('8443','KONG_SSL_CERT','IAM_GRPC_PORT','IAM_OAUTH2_TOKEN_URI')) {
  if ($config -notmatch [regex]::Escape($required)) { throw "No se encontro $required en docker compose config." }
}
Write-Host '   Compose/TLS/gRPC: OK' -ForegroundColor Green

Write-Host '2) Verificando HTTPS de Kong...' -ForegroundColor Cyan
$http = & curl.exe --cacert .\platform\certs\ca.crt -s -o NUL -w "%{http_code}" https://localhost:8443/api/v1/auth/me
if ($LASTEXITCODE -ne 0) { throw 'No se pudo conectar por HTTPS a Kong.' }
if ($http -notin @('200','401','403')) { throw "Kong HTTPS respondio HTTP $http" }
Write-Host "   Kong HTTPS: OK (HTTP $http)" -ForegroundColor Green

Write-Host '3) Verificando OAuth2 Client Credentials interno...' -ForegroundColor Cyan
$secretLine = Get-Content $Secrets | Where-Object { $_ -match '^OAUTH2_NOTIFICATION_CLIENT_SECRET=' } | Select-Object -First 1
if (-not $secretLine) { throw 'Falta OAUTH2_NOTIFICATION_CLIENT_SECRET en bootstrap.env.' }
$secret = $secretLine.Substring($secretLine.IndexOf('=') + 1)
$status = & curl.exe --cacert .\platform\certs\ca.crt -s -o NUL -w "%{http_code}" `
  -u "notification-service:$secret" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=client_credentials&scope=iam.user.read" `
  https://localhost:8443/oauth2/token
if ($LASTEXITCODE -ne 0) { throw 'Fallo la solicitud OAuth2.' }
if ($status -ne '200') { throw "OAuth2 Client Credentials respondio HTTP $status" }
Write-Host '   OAuth2 Client Credentials: OK' -ForegroundColor Green

Write-Host '4) Verificando que IAM expuso gRPC mTLS...' -ForegroundColor Cyan
$logs = docker compose --env-file .env --env-file $Secrets logs --tail=200 iam-service 2>&1 | Out-String
if ($logs -notmatch 'IAM gRPC mTLS listening on port 9091') {
  Write-Warning 'No se encontro aun el mensaje de inicio gRPC. Revise: docker compose logs iam-service'
} else {
  Write-Host '   IAM gRPC mTLS: OK' -ForegroundColor Green
}

Write-Host 'Validacion V3.6 finalizada.' -ForegroundColor Green
