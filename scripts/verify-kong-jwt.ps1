param(
  [string]$AccessToken = ''
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
  $adminPort = Get-DotEnvValue 'KONG_ADMIN_HOST_PORT' '8001'
  $httpsPort = Get-DotEnvValue 'KONG_HTTPS_HOST_PORT' '8443'
  $admin = "http://localhost:$adminPort"
  $proxy = "https://localhost:$httpsPort"

  Write-Host '1) Verificando plugins JWT declarados...' -ForegroundColor Cyan
  $plugins = (Invoke-RestMethod "$admin/plugins").data
  $jwtPlugins = @($plugins | Where-Object { $_.name -eq 'jwt' })
  if ($jwtPlugins.Count -ne 6) {
    throw "Se esperaban 6 plugins JWT por ruta y se encontraron $($jwtPlugins.Count)."
  }
  Write-Host "   OK: $($jwtPlugins.Count) rutas protegidas por JWT." -ForegroundColor Green

  Write-Host '2) Verificando consumer JWT del issuer IAM...' -ForegroundColor Cyan
  $consumers = (Invoke-RestMethod "$admin/consumers").data
  if (-not ($consumers | Where-Object { $_.username -eq 'finanscore-iam' })) {
    throw 'No existe el consumer finanscore-iam.'
  }
  Write-Host '   OK: consumer finanscore-iam presente.' -ForegroundColor Green

  Write-Host '3) Verificando que una ruta protegida rechaza ausencia de JWT...' -ForegroundColor Cyan
  $code = (& curl.exe -k -s -o NUL -w '%{http_code}' "$proxy/api/v1/query/my-requests").Trim()
  if ($code -ne '401') {
    throw "Se esperaba HTTP 401 sin JWT y se obtuvo $code."
  }
  Write-Host '   OK: sin JWT => 401 en Kong.' -ForegroundColor Green

  Write-Host '4) Verificando preflight CORS sin JWT...' -ForegroundColor Cyan
  $preflight = (& curl.exe -k -s -o NUL -w '%{http_code}' -X OPTIONS "$proxy/api/v1/query/my-requests" -H 'Origin: http://localhost:4200' -H 'Access-Control-Request-Method: GET' -H 'Access-Control-Request-Headers: Authorization').Trim()
  if ($preflight -notin @('200','204')) {
    throw "Preflight CORS inesperado: HTTP $preflight."
  }
  Write-Host "   OK: OPTIONS => $preflight sin exigir JWT." -ForegroundColor Green

  if ($AccessToken) {
    Write-Host '5) Probando Access JWT proporcionado...' -ForegroundColor Cyan
    $validCode = (& curl.exe -k -s -o NUL -w '%{http_code}' "$proxy/api/v1/query/my-requests" -H "Authorization: Bearer $AccessToken").Trim()
    Write-Host "   Respuesta con JWT: HTTP $validCode" -ForegroundColor Yellow
    if ($validCode -eq '401') {
      throw 'El Access JWT fue rechazado por Kong o por el microservicio.'
    }
  }

  Write-Host ''
  Write-Host 'Validacion Kong JWT OK.' -ForegroundColor Green
  exit 0
} catch {
  Write-Host ''
  Write-Host ('Validacion Kong JWT FALLIDA: ' + $_.Exception.Message) -ForegroundColor Red
  exit 1
}
