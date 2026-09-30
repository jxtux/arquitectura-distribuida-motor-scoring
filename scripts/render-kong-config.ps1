$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Template = Join-Path $Root 'gateway\kong\kong.template.yml'
$Output = Join-Path $Root 'gateway\kong\kong.yml'
$PublicKey = Join-Path $Root 'platform\certs\iam-public.pem'
$CaCert = Join-Path $Root 'platform\certs\ca.crt'

if (-not (Test-Path $Template)) { throw "Falta $Template" }
if (-not (Test-Path $PublicKey)) { throw "Falta $PublicKey. Ejecute .\scripts\generate-dev-certs.ps1" }
if (-not (Test-Path $CaCert)) { throw "Falta $CaCert. Ejecute .\scripts\generate-dev-certs.ps1" }

$templateText = Get-Content $Template -Raw
if ($templateText -notmatch '__IAM_PUBLIC_KEY_PEM__') { throw 'kong.template.yml no contiene __IAM_PUBLIC_KEY_PEM__.' }
if ($templateText -notmatch '__INTERNAL_CA_PEM__') { throw 'kong.template.yml no contiene __INTERNAL_CA_PEM__.' }

$pemLines = Get-Content $PublicKey
if (-not ($pemLines -contains '-----BEGIN PUBLIC KEY-----') -or -not ($pemLines -contains '-----END PUBLIC KEY-----')) {
  throw 'iam-public.pem no tiene formato PEM PUBLIC KEY esperado.'
}
$caLines = Get-Content $CaCert
if (-not ($caLines -contains '-----BEGIN CERTIFICATE-----') -or -not ($caLines -contains '-----END CERTIFICATE-----')) {
  throw 'ca.crt no tiene formato PEM CERTIFICATE esperado.'
}

# El bloque JWT esta dentro de jwt_secrets: seis espacios para el scalar YAML.
$indentedPem = ($pemLines | ForEach-Object { '      ' + $_ }) -join [Environment]::NewLine
# El CA Certificate es entidad de nivel superior: cuatro espacios para el scalar YAML.
$indentedCa = ($caLines | ForEach-Object { '    ' + $_ }) -join [Environment]::NewLine
$rendered = $templateText.Replace('__IAM_PUBLIC_KEY_PEM__', $indentedPem).Replace('__INTERNAL_CA_PEM__', $indentedCa)
Set-Content -Path $Output -Value $rendered -Encoding utf8
Write-Host 'Kong declarative config rendered: JWT RSA + trusted internal CA for HTTPS upstreams.' -ForegroundColor Green
