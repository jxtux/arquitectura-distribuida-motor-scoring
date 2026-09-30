param(
  [int]$WaitSeconds = 180,
  [int]$IntervalSeconds = 5
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

try {
  $kongAdminPort = Get-DotEnvValue 'KONG_ADMIN_HOST_PORT' '8001'
  $promPort = Get-DotEnvValue 'PROMETHEUS_HOST_PORT' '9090'
  $kongAdmin = "http://localhost:$kongAdminPort"
  $prom = "http://localhost:$promPort"

  Write-Host '1) Verificando HTTPS + validacion TLS en los upstreams de Kong...' -ForegroundColor Cyan
  $expected = @('iam-service','credit-service','payment-service','report-service','query-service','audit-service')
  $services = @((Invoke-RestMethod "$kongAdmin/services").data)
  $rows = foreach ($name in $expected) {
    $svc = $services | Where-Object { $_.name -eq $name } | Select-Object -First 1
    if (-not $svc) { throw "Kong no contiene el servicio $name." }
    $caCount = @($svc.ca_certificates).Count
    [pscustomobject]@{
      Service = $name
      Protocol = $svc.protocol
      TlsVerify = [bool]$svc.tls_verify
      CaCertificates = $caCount
      Ok = ($svc.protocol -eq 'https' -and [bool]$svc.tls_verify -and $caCount -ge 1)
    }
  }
  $rows | Select-Object Service,Protocol,TlsVerify,CaCertificates | Format-Table -AutoSize
  $bad = @($rows | Where-Object { -not $_.Ok })
  if ($bad.Count -gt 0) { throw 'Hay upstreams de Kong sin HTTPS/TLS verify/CA.' }
  Write-Host '   OK: los 6 upstreams de Kong usan HTTPS con verificacion de certificado.' -ForegroundColor Green

  Write-Host '2) Verificando que Prometheus pueda validar TLS y scrapear los 8 microservicios...' -ForegroundColor Cyan
  $deadline = (Get-Date).AddSeconds($WaitSeconds)
  $result = @()
  do {
    try {
      $response = Invoke-RestMethod "$prom/api/v1/query?query=up%7Bjob%3D%22finanscore-services%22%7D"
      $result = @($response.data.result)
      $up = @($result | Where-Object { $_.value[1] -eq '1' })
      if ($result.Count -eq 8 -and $up.Count -eq 8) { break }
    } catch {
      $result = @()
    }
    Start-Sleep -Seconds $IntervalSeconds
  } while ((Get-Date) -lt $deadline)

  $metricRows = foreach ($item in $result) {
    [pscustomobject]@{ Instance=$item.metric.instance; Up=$item.value[1] }
  }
  if ($metricRows.Count -gt 0) { $metricRows | Sort-Object Instance | Format-Table -AutoSize }
  $upCount = @($result | Where-Object { $_.value[1] -eq '1' }).Count
  if ($result.Count -ne 8 -or $upCount -ne 8) {
    throw "Prometheus no confirma HTTPS saludable en los 8 servicios (series=$($result.Count), up=$upCount)."
  }
  Write-Host '   OK: 8/8 microservicios responden a Prometheus por HTTPS con CA verificada.' -ForegroundColor Green

  Write-Host ''
  Write-Host 'Validacion HTTPS interno V3.6.8.1 OK.' -ForegroundColor Green
  exit 0
} catch {
  Write-Host ''
  Write-Host ('Validacion HTTPS interno FALLIDA: ' + $_.Exception.Message) -ForegroundColor Red
  exit 1
}
