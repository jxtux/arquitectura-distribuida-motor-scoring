$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Ca = Join-Path $Root 'platform\certs\ca.crt'
if (-not (Test-Path $Ca)) {
  throw 'No existe platform\certs\ca.crt. Ejecute primero .\scripts\generate-dev-certs.ps1'
}
Write-Host 'Instalando la CA de desarrollo en Trusted Root Certification Authorities...' -ForegroundColor Cyan
& certutil -addstore -f Root $Ca
if ($LASTEXITCODE -ne 0) {
  throw 'certutil fallo. Abra PowerShell como Administrador y vuelva a ejecutar este script.'
}
Write-Host 'CA de desarrollo instalada. Cierre y vuelva a abrir el navegador si fuera necesario.' -ForegroundColor Green
