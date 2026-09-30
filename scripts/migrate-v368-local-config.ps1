param(
  [string]$SourceRoot = 'D:\motor-scoring-distributed-v3-6-8'
)
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

if (Test-Path "$SourceRoot\.env") { Copy-Item "$SourceRoot\.env" '.\.env' -Force }
if (Test-Path "$SourceRoot\secrets\bootstrap.env") { Copy-Item "$SourceRoot\secrets\bootstrap.env" '.\secrets\bootstrap.env' -Force }
if (Test-Path "$SourceRoot\platform\certs") { Copy-Item "$SourceRoot\platform\certs\*" '.\platform\certs\' -Force }
Write-Host 'Migracion V3.6.8 -> V3.6.8.1 lista: .env, bootstrap.env y certificados conservados.' -ForegroundColor Green
