param(
  [string]$SourceRoot = 'D:\motor-scoring-distributed-v3-6-7'
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

Copy-Item $sourceEnv (Join-Path $Root '.env') -Force
New-Item -ItemType Directory -Force -Path (Join-Path $Root 'secrets') | Out-Null
Copy-Item $sourceBootstrap (Join-Path $Root 'secrets\bootstrap.env') -Force
New-Item -ItemType Directory -Force -Path (Join-Path $Root 'platform\certs') | Out-Null
Copy-Item (Join-Path $sourceCerts '*') (Join-Path $Root 'platform\certs') -Force -Recurse

Write-Host 'Migracion V3.6.7 -> V3.6.8 lista: .env, bootstrap.env y certificados conservados.' -ForegroundColor Green
