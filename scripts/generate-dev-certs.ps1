$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$SecretsFile = Join-Path $Root 'secrets\bootstrap.env'
if (-not (Test-Path $SecretsFile)) { throw 'Falta secrets/bootstrap.env. Ejecute .\scripts\prepare-local-secrets.ps1' }
Get-Content $SecretsFile | ForEach-Object {
  if ($_ -match '^([^#][^=]*)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process') }
}
$Pass = $env:DEV_CERT_PASSWORD
if (-not $Pass) { throw 'DEV_CERT_PASSWORD no definido.' }
$Out = Join-Path $Root 'platform\certs'
New-Item -ItemType Directory -Force -Path $Out | Out-Null
Get-ChildItem $Out -File | Where-Object { $_.Name -notin @('.gitignore','README.md') } | Remove-Item -Force
function Run([string]$exe, [string[]]$a) { & $exe @a; if ($LASTEXITCODE -ne 0) { throw "$exe fallo." } }
Run 'openssl' @('genrsa','-out',"$Out\ca.key",'4096')
Run 'openssl' @('req','-x509','-new','-nodes','-key',"$Out\ca.key",'-sha256','-days','3650','-subj','/CN=MotorScoring Development CA','-out',"$Out\ca.crt")
function Gen-Cert($name,$cn,$san,$eku='serverAuth') {
  Run 'openssl' @('genrsa','-out',"$Out\$name.key",'2048')
  Run 'openssl' @('req','-new','-key',"$Out\$name.key",'-subj',"/CN=$cn",'-out',"$Out\$name.csr")
  @("subjectAltName=$san","extendedKeyUsage=$eku",'keyUsage=digitalSignature,keyEncipherment') | Set-Content "$Out\$name.ext" -Encoding ascii
  Run 'openssl' @('x509','-req','-in',"$Out\$name.csr",'-CA',"$Out\ca.crt",'-CAkey',"$Out\ca.key",'-CAcreateserial','-out',"$Out\$name.crt",'-days','825','-sha256','-extfile',"$Out\$name.ext")
}
Gen-Cert 'kafka-server' 'kafka' 'DNS:kafka,DNS:localhost,IP:127.0.0.1' 'serverAuth,clientAuth'
Gen-Cert 'api-server' 'motor-scoring-api' 'DNS:motor-scoring-api,DNS:localhost,IP:127.0.0.1'
Gen-Cert 'postgres-server' 'postgres' 'DNS:postgres,DNS:localhost,IP:127.0.0.1'
Gen-Cert 'kong-server' 'kong' 'DNS:kong,DNS:localhost,IP:127.0.0.1' 'serverAuth'
Gen-Cert 'iam-grpc-server' 'iam-service' 'DNS:iam-service,DNS:localhost,IP:127.0.0.1' 'serverAuth'
Gen-Cert 'notification-grpc-client' 'notification-service' 'DNS:notification-service' 'clientAuth'
Run 'openssl' @('pkcs12','-export','-name','kafka','-in',"$Out\kafka-server.crt",'-inkey',"$Out\kafka-server.key",'-certfile',"$Out\ca.crt",'-out',"$Out\kafka-server.p12",'-password',"pass:$Pass")
Run 'keytool' @('-importkeystore','-noprompt','-srckeystore',"$Out\kafka-server.p12",'-srcstoretype','PKCS12','-srcstorepass',$Pass,'-destkeystore',"$Out\kafka.keystore.jks",'-deststoretype','JKS','-deststorepass',$Pass)
Run 'keytool' @('-importcert','-noprompt','-alias','motor-scoring-ca','-file',"$Out\ca.crt",'-keystore',"$Out\kafka.truststore.jks",'-storepass',$Pass)
Run 'keytool' @('-importcert','-noprompt','-alias','motor-scoring-ca','-file',"$Out\ca.crt",'-keystore',"$Out\client.truststore.jks",'-storepass',$Pass)
Run 'openssl' @('pkcs12','-export','-name','motor-scoring-api','-in',"$Out\api-server.crt",'-inkey',"$Out\api-server.key",'-certfile',"$Out\ca.crt",'-out',"$Out\api-keystore.p12",'-password',"pass:$Pass")
Run 'openssl' @('genpkey','-algorithm','RSA','-pkeyopt','rsa_keygen_bits:3072','-out',"$Out\iam-private.pem")
Run 'openssl' @('rsa','-pubout','-in',"$Out\iam-private.pem",'-out',"$Out\iam-public.pem")
$totp = & openssl rand -base64 32; Set-Content "$Out\iam-totp-aes.key" $totp -Encoding ascii
$pepper = & openssl rand -hex 32; Set-Content "$Out\iam-verification-pepper.txt" $pepper -Encoding ascii
Get-ChildItem $Out -Filter '*.csr' | Remove-Item -Force; Get-ChildItem $Out -Filter '*.ext' | Remove-Item -Force; Get-ChildItem $Out -Filter '*.srl' | Remove-Item -Force
'Certificados y secretos IAM de desarrollo. NO usar en produccion.' | Set-Content "$Out\README.txt" -Encoding ascii
Write-Host 'TLS + IAM development material ready.' -ForegroundColor Green
& (Join-Path $Root 'scripts\generate-service-https-certs.ps1') -Force
if (Test-Path (Join-Path $Root 'scripts\render-kong-config.ps1')) {
  & (Join-Path $Root 'scripts\render-kong-config.ps1')
}
