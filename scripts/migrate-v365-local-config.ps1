param(
  [string]$SourceRoot = 'D:\motor-scoring-distributed-v3-6-5'
)
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$envSource = Join-Path $SourceRoot '.env'
$bootstrapSource = Join-Path $SourceRoot 'secrets\bootstrap.env'
$certsSource = Join-Path $SourceRoot 'platform\certs'

foreach ($required in @($envSource,$bootstrapSource,$certsSource)) {
  if (-not (Test-Path $required)) { throw "No existe: $required" }
}

Copy-Item $envSource '.\.env' -Force
New-Item -ItemType Directory -Force -Path '.\secrets' | Out-Null
Copy-Item $bootstrapSource '.\secrets\bootstrap.env' -Force
New-Item -ItemType Directory -Force -Path '.\platform\certs' | Out-Null
Copy-Item (Join-Path $certsSource '*') '.\platform\certs\' -Force

foreach ($requiredCert in @('ca.crt','postgres-server.crt','postgres-server.key','iam-public.pem','client.truststore.jks')) {
  if (-not (Test-Path (Join-Path '.\platform\certs' $requiredCert))) {
    throw "La migracion copio certificados, pero falta $requiredCert. Ejecute .\scripts\generate-dev-certs.ps1 si desea regenerar el material de desarrollo."
  }
}

Write-Host 'Configuracion V3.6.5 copiada a V3.6.6: .env, bootstrap.env y certificados.' -ForegroundColor Green
Write-Host 'Nota: los volumenes Docker no se copian; V3.6.6 usa volumenes propios por su nuevo project name.' -ForegroundColor Yellow
