$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$SecretsFile = Join-Path $Root 'secrets\bootstrap.env'
$EnvFile = Join-Path $Root '.env'
if (-not (Test-Path $SecretsFile)) { throw 'Falta secrets/bootstrap.env. Ejecute prepare-local-secrets.ps1.' }

$values = @{}
Get-Content $SecretsFile | ForEach-Object {
  if ($_ -match '^([^#][^=]*)=(.*)$') { $values[$matches[1]] = $matches[2] }
}
$public = @{}
if (Test-Path $EnvFile) {
  Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') { $public[$matches[1]] = $matches[2] }
  }
}

Write-Host '=== Motor Scoring V3.6 - accesos locales ===' -ForegroundColor Cyan
Write-Host 'Sistema:  http://localhost:4200'
Write-Host 'Kong API: https://localhost:8443'
Write-Host 'Vault:    http://localhost:8200'
Write-Host 'Grafana:  http://localhost:3000'
Write-Host ''
$adminEmail = if ($public.ContainsKey('IAM_ADMIN_EMAIL') -and $public['IAM_ADMIN_EMAIL']) { $public['IAM_ADMIN_EMAIL'] } else { 'admin@finanscore.local' }
Write-Host ('ADMIN_EMAIL=' + $adminEmail)
Write-Host ('IAM_ADMIN_PASSWORD=' + $values['IAM_ADMIN_PASSWORD'])
Write-Host 'DEMO_USERS=demo1@finanscore.local ... demo5@finanscore.local'
Write-Host ('IAM_DEMO_PASSWORD=' + $values['IAM_DEMO_PASSWORD'])
$grafanaUser = if ($public.ContainsKey('GRAFANA_ADMIN_USER') -and $public['GRAFANA_ADMIN_USER']) { $public['GRAFANA_ADMIN_USER'] } else { 'admin' }
Write-Host ('GRAFANA_USER=' + $grafanaUser)
Write-Host ('GRAFANA_ADMIN_PASSWORD=' + $values['GRAFANA_ADMIN_PASSWORD'])
Write-Host ('VAULT_DEV_ROOT_TOKEN_ID=' + $values['VAULT_DEV_ROOT_TOKEN_ID'])
Write-Host ''
Write-Host 'El secreto OAuth2 interno no se imprime deliberadamente.' -ForegroundColor DarkGray
