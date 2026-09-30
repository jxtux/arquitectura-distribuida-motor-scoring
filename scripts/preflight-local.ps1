param(
  [switch]$Quiet
)
$ErrorActionPreference = 'Stop'
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

$ports = @(
  @{ Var='POSTGRES_HOST_PORT'; Default='5432'; Service='PostgreSQL' },
  @{ Var='KAFKA_HOST_PORT'; Default='9092'; Service='Kafka' },
  @{ Var='SCHEMA_REGISTRY_HOST_PORT'; Default='8088'; Service='Schema Registry' },
  @{ Var='MINIO_API_HOST_PORT'; Default='9000'; Service='MinIO API' },
  @{ Var='MINIO_CONSOLE_HOST_PORT'; Default='9001'; Service='MinIO Console' },
  @{ Var='VAULT_HOST_PORT'; Default='8200'; Service='Vault' },
  @{ Var='KONG_HTTPS_HOST_PORT'; Default='8443'; Service='Kong HTTPS' },
  @{ Var='KONG_ADMIN_HOST_PORT'; Default='8001'; Service='Kong Admin' },
  @{ Var='FRONTEND_HOST_PORT'; Default='4200'; Service='Angular' },
  @{ Var='PROMETHEUS_HOST_PORT'; Default='9090'; Service='Prometheus' },
  @{ Var='GRAFANA_HOST_PORT'; Default='3000'; Service='Grafana' },
  @{ Var='LOKI_HOST_PORT'; Default='3100'; Service='Loki' },
  @{ Var='TEMPO_HOST_PORT'; Default='3200'; Service='Tempo' },
  @{ Var='OTEL_GRPC_HOST_PORT'; Default='4317'; Service='OTel gRPC' },
  @{ Var='OTEL_HTTP_HOST_PORT'; Default='4318'; Service='OTel HTTP' },
  @{ Var='OTEL_HEALTH_HOST_PORT'; Default='13133'; Service='OTel Health' },
  @{ Var='ALLOY_HOST_PORT'; Default='12345'; Service='Alloy' }
)

$currentPrefix = 'motor-scoring-distributed-v3-6-8-1-'
$conflicts = @()
foreach ($item in $ports) {
  $port = [int](Get-DotEnvValue $item.Var $item.Default)
  $dockerNames = @(& docker ps --filter "publish=$port" --format '{{.Names}}' 2>$null)
  $dockerNames = @($dockerNames | Where-Object { $_ -and ($_ -notlike "$currentPrefix*") })
  if ($dockerNames.Count -gt 0) {
    $conflicts += [pscustomobject]@{ Service=$item.Service; Port=$port; Variable=$item.Var; Owner=('Docker: ' + ($dockerNames -join ', ')) }
    continue
  }

  # Si el puerto no esta publicado por Docker, revisa procesos del host.
  try {
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
    if ($listeners.Count -gt 0) {
      $pids = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique)
      $owners = foreach ($pidValue in $pids) {
        try { (Get-Process -Id $pidValue -ErrorAction Stop).ProcessName + " (PID $pidValue)" }
        catch { "PID $pidValue" }
      }
      $conflicts += [pscustomobject]@{ Service=$item.Service; Port=$port; Variable=$item.Var; Owner=('Host: ' + ($owners -join ', ')) }
    }
  } catch {
    # Get-NetTCPConnection puede no estar disponible fuera de Windows; Docker ya cubre el caso principal.
  }
}

if ($conflicts.Count -gt 0) {
  Write-Host ''
  Write-Host 'PUERTOS OCUPADOS - no se iniciara el stack para evitar un arranque parcial.' -ForegroundColor Red
  $conflicts | Format-Table -AutoSize
  Write-Host ''
  Write-Host 'Solucion recomendada:' -ForegroundColor Yellow
  Write-Host '  1) Detenga la version anterior del Motor de Scoring; o'
  Write-Host '  2) Cambie en .env la variable *_HOST_PORT indicada arriba.'
  Write-Host ''
  Write-Host 'Para localizar contenedores antiguos: docker compose ls'
  exit 2
}

if (-not $Quiet) {
  Write-Host 'Preflight OK: los puertos requeridos estan disponibles.' -ForegroundColor Green
}
exit 0
