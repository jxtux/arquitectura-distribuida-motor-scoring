param([switch]$Force)
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Dir = Join-Path $Root 'secrets'
$Out = Join-Path $Dir 'bootstrap.env'
New-Item -ItemType Directory -Force -Path $Dir | Out-Null
if ((Test-Path $Out) -and -not $Force) {
  Write-Host "El archivo $Out ya existe. Use -Force para regenerarlo." -ForegroundColor Yellow
  exit 0
}
function New-Secret([int]$Bytes = 24) {
  $buffer = New-Object byte[] $Bytes
  [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
  ([System.BitConverter]::ToString($buffer)).Replace('-', '').ToLowerInvariant()
}
function New-Id { [guid]::NewGuid().ToString('N') }
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add('# Generado localmente. NO VERSIONAR. V3.6')
foreach ($name in @(
'POSTGRES_SUPER_PASSWORD','IAM_DB_PASSWORD','CREDIT_DB_PASSWORD','PAYMENT_DB_PASSWORD','SCORING_DB_PASSWORD','REPORT_DB_PASSWORD','NOTIFICATION_DB_PASSWORD','AUDIT_DB_PASSWORD','QUERY_DB_PASSWORD',
'DEV_CERT_PASSWORD','KAFKA_ADMIN_PASSWORD','KAFKA_BROKER_PASSWORD','KAFKA_IAM_PASSWORD','KAFKA_CREDIT_PASSWORD','KAFKA_PAYMENT_PASSWORD','KAFKA_SCORING_PASSWORD','KAFKA_REPORT_PASSWORD','KAFKA_NOTIFICATION_PASSWORD','KAFKA_AUDIT_PASSWORD','KAFKA_QUERY_PASSWORD','KAFKA_SCHEMA_REGISTRY_PASSWORD',
'REDIS_PASSWORD','MINIO_ROOT_PASSWORD','GRAFANA_ADMIN_PASSWORD','IAM_DEMO_PASSWORD','IAM_ADMIN_PASSWORD','OAUTH2_NOTIFICATION_CLIENT_SECRET','VAULT_DEV_ROOT_TOKEN_ID')) {
  $lines.Add("$name=$(New-Secret 24)")
}
$lines.Add('GOOGLE_CLIENT_SECRET=')
$lines.Add('TIKTOK_CLIENT_SECRET=')
$lines.Add('GMAIL_APP_PASSWORD=')
foreach ($svc in @('IAM','CREDIT','PAYMENT','SCORING','REPORT','NOTIFICATION','AUDIT','QUERY')) {
  $lines.Add("VAULT_${svc}_ROLE_ID=$(New-Id)")
  $lines.Add("VAULT_${svc}_SECRET_ID=$(New-Secret 32)")
}
Set-Content -Path $Out -Value $lines -Encoding ascii
Write-Host "Secretos locales generados en $Out" -ForegroundColor Green
Write-Host 'Edite GOOGLE_CLIENT_SECRET / TIKTOK_CLIENT_SECRET / GMAIL_APP_PASSWORD si desea habilitar esas integraciones.'
