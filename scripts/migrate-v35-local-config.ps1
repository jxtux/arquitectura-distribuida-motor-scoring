$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

if (-not (Test-Path '.env')) {
  Copy-Item '.env.example' '.env'
}

function Set-EnvValue([string]$Path,[string]$Name,[string]$Value) {
  $lines = @(Get-Content $Path)
  $pattern = '^' + [regex]::Escape($Name) + '='
  $found = $false
  $lines = $lines | ForEach-Object {
    if ($_ -match $pattern) { $found=$true; "$Name=$Value" } else { $_ }
  }
  if (-not $found) { $lines += "$Name=$Value" }
  Set-Content -Path $Path -Value $lines -Encoding ascii
}

Set-EnvValue '.env' 'IAM_REFRESH_COOKIE_SECURE' 'true'
Set-EnvValue '.env' 'IAM_REFRESH_COOKIE_SAME_SITE' 'None'

if (-not (Test-Path 'secrets\bootstrap.env')) {
  & .\scripts\prepare-local-secrets.ps1
} elseif (-not (Select-String -Path 'secrets\bootstrap.env' -Pattern '^OAUTH2_NOTIFICATION_CLIENT_SECRET=' -Quiet)) {
  $buffer = New-Object byte[] 24
  [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
  $secret = ([System.BitConverter]::ToString($buffer)).Replace('-', '').ToLowerInvariant()
  Add-Content -Path 'secrets\bootstrap.env' -Value "OAUTH2_NOTIFICATION_CLIENT_SECRET=$secret" -Encoding ascii
  Write-Host 'Agregado OAUTH2_NOTIFICATION_CLIENT_SECRET al bootstrap existente.' -ForegroundColor Green
}

Write-Host 'Regenerando certificados V3.6 (Kong + IAM gRPC + Notification client)...' -ForegroundColor Cyan
& .\scripts\generate-dev-certs.ps1

Write-Host ''
Write-Host 'Migracion local V3.5 -> V3.6 lista.' -ForegroundColor Green
Write-Host 'Revise en Google Cloud este redirect URI:' -ForegroundColor Yellow
Write-Host '  https://localhost:8443/login/oauth2/code/google'
Write-Host 'Y mantenga TIKTOK_REDIRECT_URI apuntando a su dominio HTTPS de ngrok.'
Write-Host 'Luego, como Administrador: .\scripts\install-dev-ca.ps1'
