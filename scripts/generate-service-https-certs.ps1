param([switch]$Force)
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$SecretsFile = Join-Path $Root 'secrets\bootstrap.env'
$Out = Join-Path $Root 'platform\certs'
$CaCert = Join-Path $Out 'ca.crt'
$CaKey = Join-Path $Out 'ca.key'

if (-not (Test-Path $SecretsFile)) { throw 'Falta secrets/bootstrap.env.' }
Get-Content $SecretsFile | ForEach-Object {
  if ($_ -match '^([^#][^=]*)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
}
$Pass = $env:DEV_CERT_PASSWORD
if (-not $Pass) { throw 'DEV_CERT_PASSWORD no definido.' }
if (-not (Test-Path $CaCert) -or -not (Test-Path $CaKey)) {
  throw 'Falta ca.crt/ca.key. Ejecute primero .\scripts\generate-dev-certs.ps1 o copie platform\certs desde V3.6.6.'
}

function Run([string]$exe, [string[]]$a) {
  & $exe @a
  if ($LASTEXITCODE -ne 0) { throw "$exe fallo." }
}

$services = @(
  'iam-service',
  'credit-service',
  'payment-service',
  'scoring-service',
  'report-service',
  'notification-service',
  'audit-service',
  'query-service'
)

foreach ($svc in $services) {
  $base = Join-Path $Out "$svc-https"
  $p12 = "$base.p12"
  if ((Test-Path $p12) -and -not $Force) {
    Write-Host "Ya existe $svc-https.p12; se conserva." -ForegroundColor DarkGray
    continue
  }

  Run 'openssl' @('genrsa','-out',"$base.key",'2048')
  Run 'openssl' @('req','-new','-key',"$base.key",'-subj',"/CN=$svc",'-out',"$base.csr")
  @(
    "subjectAltName=DNS:$svc,DNS:localhost,IP:127.0.0.1",
    'extendedKeyUsage=serverAuth',
    'keyUsage=digitalSignature,keyEncipherment'
  ) | Set-Content "$base.ext" -Encoding ascii
  Run 'openssl' @('x509','-req','-in',"$base.csr",'-CA',$CaCert,'-CAkey',$CaKey,'-CAcreateserial','-out',"$base.crt",'-days','825','-sha256','-extfile',"$base.ext")
  Run 'openssl' @('pkcs12','-export','-name',$svc,'-in',"$base.crt",'-inkey',"$base.key",'-certfile',$CaCert,'-out',$p12,'-password',"pass:$Pass")
  Remove-Item "$base.csr","$base.ext" -Force -ErrorAction SilentlyContinue
  Write-Host "HTTPS certificate ready: $svc" -ForegroundColor Green
}

Get-ChildItem $Out -Filter '*.srl' | Remove-Item -Force -ErrorAction SilentlyContinue
Write-Host 'Certificados HTTPS internos de microservicios listos.' -ForegroundColor Green
