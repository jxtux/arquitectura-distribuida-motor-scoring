param(
  [int]$WaitSeconds = 180,
  [int]$IntervalSeconds = 5
)
$ErrorActionPreference = 'Continue'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

function Get-DotEnvValue([string]$Name, [string]$DefaultValue) {
  if (Test-Path '.env') {
    $line = Get-Content '.env' | Where-Object { $_ -match ('^\s*' + [regex]::Escape($Name) + '\s*=') } | Select-Object -Last 1
    if ($line) {
      $value = ($line -replace ('^\s*' + [regex]::Escape($Name) + '\s*=\s*'), '').Trim().Trim('"').Trim("'")
      if ($value) { return $value }
    }
  }
  return $DefaultValue
}

function Test-HttpEndpoint([string]$Name, [string]$Url, [int[]]$Accepted = @(200)) {
  try {
    $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 4
    return [pscustomobject]@{ Name=$Name; Url=$Url; Ok=($Accepted -contains [int]$response.StatusCode); Status=[int]$response.StatusCode }
  } catch {
    return [pscustomobject]@{ Name=$Name; Url=$Url; Ok=$false; Status='NO RESPONSE' }
  }
}

$minioPort = Get-DotEnvValue 'MINIO_API_HOST_PORT' '9000'
$vaultPort = Get-DotEnvValue 'VAULT_HOST_PORT' '8200'
$kongAdminPort = Get-DotEnvValue 'KONG_ADMIN_HOST_PORT' '8001'
$frontendPort = Get-DotEnvValue 'FRONTEND_HOST_PORT' '4200'
$promPort = Get-DotEnvValue 'PROMETHEUS_HOST_PORT' '9090'
$grafanaPort = Get-DotEnvValue 'GRAFANA_HOST_PORT' '3000'
$lokiPort = Get-DotEnvValue 'LOKI_HOST_PORT' '3100'
$tempoPort = Get-DotEnvValue 'TEMPO_HOST_PORT' '3200'
$otelPort = Get-DotEnvValue 'OTEL_HEALTH_HOST_PORT' '13133'
$alloyPort = Get-DotEnvValue 'ALLOY_HOST_PORT' '12345'

$checks = @(
  @{ Name='MinIO'; Url="http://localhost:$minioPort/minio/health/live"; Accepted=@(200) },
  @{ Name='Vault'; Url="http://localhost:$vaultPort/v1/sys/health"; Accepted=@(200) },
  @{ Name='Kong Admin'; Url="http://localhost:$kongAdminPort/status"; Accepted=@(200) },
  @{ Name='Angular'; Url="http://localhost:$frontendPort/"; Accepted=@(200) },
  @{ Name='Prometheus'; Url="http://localhost:$promPort/-/ready"; Accepted=@(200) },
  @{ Name='Grafana'; Url="http://localhost:$grafanaPort/api/health"; Accepted=@(200) },
  @{ Name='Loki'; Url="http://localhost:$lokiPort/ready"; Accepted=@(200) },
  @{ Name='Tempo'; Url="http://localhost:$tempoPort/ready"; Accepted=@(200) },
  @{ Name='OTel Collector'; Url="http://localhost:$otelPort/"; Accepted=@(200) },
  @{ Name='Alloy'; Url="http://localhost:$alloyPort/-/ready"; Accepted=@(200) }
)

$deadline = (Get-Date).AddSeconds($WaitSeconds)
$results = @()
do {
  $results = foreach ($check in $checks) {
    Test-HttpEndpoint $check.Name $check.Url $check.Accepted
  }
  if ((@($results | Where-Object { -not $_.Ok })).Count -eq 0) { break }
  Start-Sleep -Seconds $IntervalSeconds
} while ((Get-Date) -lt $deadline)

Write-Host ''
Write-Host 'Estado HTTP del stack local:' -ForegroundColor Cyan
$results | Select-Object Name,Status,Url | Format-Table -AutoSize
Write-Host ''
Write-Host 'Estado Docker Compose:' -ForegroundColor Cyan
docker compose --env-file .env --env-file secrets/bootstrap.env ps -a

$failed = @($results | Where-Object { -not $_.Ok })
if ($failed.Count -gt 0) {
  Write-Host ''
  Write-Host ('No respondieron correctamente: ' + (($failed | Select-Object -ExpandProperty Name) -join ', ')) -ForegroundColor Red
  Write-Host 'Revise: docker compose --env-file .env --env-file secrets/bootstrap.env logs --tail=120 <servicio>' -ForegroundColor Yellow
  exit 1
}

Write-Host ''
Write-Host 'Validando HTTPS/TLS interno entre Kong y microservicios...' -ForegroundColor Cyan
& .\scripts\verify-internal-https.ps1 -WaitSeconds $WaitSeconds -IntervalSeconds $IntervalSeconds
if ($LASTEXITCODE -ne 0) {
  Write-Host 'Fallo la validacion HTTPS interna de microservicios.' -ForegroundColor Red
  exit 1
}

Write-Host ''
Write-Host 'Validando TLS obligatorio de PostgreSQL...' -ForegroundColor Cyan
& .\scripts\verify-postgres-tls.ps1
if ($LASTEXITCODE -ne 0) {
  Write-Host 'Fallo la validacion TLS de PostgreSQL.' -ForegroundColor Red
  exit 1
}

Write-Host ''
Write-Host 'Validando capa JWT perimetral de Kong...' -ForegroundColor Cyan
& .\scripts\verify-kong-jwt.ps1
if ($LASTEXITCODE -ne 0) {
  Write-Host 'Fallo la validacion JWT de Kong.' -ForegroundColor Red
  exit 1
}

Write-Host ''
Write-Host 'Validacion OK: endpoints, HTTPS interno, observabilidad, PostgreSQL TLS y JWT perimetral de Kong responden.' -ForegroundColor Green
exit 0
