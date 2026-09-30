param(
  [string]$SourceRoot = 'D:\motor-scoring-distributed-v3-6-6'
)
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

if (-not (Test-Path $SourceRoot)) { throw "No existe SourceRoot: $SourceRoot" }
$sourceEnv = Join-Path $SourceRoot '.env'
$sourceBootstrap = Join-Path $SourceRoot 'secrets\bootstrap.env'
$sourceCerts = Join-Path $SourceRoot 'platform\certs'
if (-not (Test-Path $sourceEnv)) { throw "Falta $sourceEnv" }
if (-not (Test-Path $sourceBootstrap)) { throw "Falta $sourceBootstrap" }
if (-not (Test-Path (Join-Path $sourceCerts 'ca.crt'))) { throw "Falta ca.crt en $sourceCerts" }
if (-not (Test-Path (Join-Path $sourceCerts 'ca.key'))) { throw "Falta ca.key en $sourceCerts" }

Copy-Item $sourceEnv (Join-Path $Root '.env') -Force
New-Item -ItemType Directory -Force -Path (Join-Path $Root 'secrets') | Out-Null
Copy-Item $sourceBootstrap (Join-Path $Root 'secrets\bootstrap.env') -Force
New-Item -ItemType Directory -Force -Path (Join-Path $Root 'platform\certs') | Out-Null
Copy-Item (Join-Path $sourceCerts '*') (Join-Path $Root 'platform\certs') -Force -Recurse

Set-Location $Root
& .\scripts\generate-service-https-certs.ps1
& .\scripts\render-kong-config.ps1
Write-Host 'Migracion V3.6.6 -> V3.6.7 lista: .env, bootstrap.env y CA existentes conservados; certificados HTTPS internos agregados.' -ForegroundColor Green
