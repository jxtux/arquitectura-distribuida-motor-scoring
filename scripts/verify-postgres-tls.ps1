$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$compose = @(
    'compose',
    '--env-file', '.env',
    '--env-file', 'secrets/bootstrap.env'
)

function Get-BootstrapValue([string]$Name) {

    $line = Get-Content 'secrets/bootstrap.env' |
        Where-Object {
            $_ -match ('^' + [regex]::Escape($Name) + '=')
        } |
        Select-Object -First 1

    if (-not $line) {
        throw "No se encontro $Name en secrets/bootstrap.env"
    }

    return $line.Substring($Name.Length + 1).Trim()
}

$postgresPassword = Get-BootstrapValue 'POSTGRES_SUPER_PASSWORD'

# ============================================================
# 1. COMPROBAR QUE POSTGRESQL TIENE SSL HABILITADO
# ============================================================

Write-Host ''
Write-Host 'PostgreSQL TLS - comprobando ssl=on...' -ForegroundColor Cyan

$sslLines = & docker @compose exec -T `
    postgres `
    psql `
    -U postgres `
    -d postgres `
    -tAc 'SHOW ssl;' `
    2>&1

$sslExit = $LASTEXITCODE
$ssl = ($sslLines | Out-String).Trim()

if ($sslExit -ne 0 -or $ssl -notmatch '(?im)^on$') {

    Write-Host `
        "ERROR: PostgreSQL no reporta ssl=on. Respuesta: $ssl" `
        -ForegroundColor Red

    exit 1
}

Write-Host 'OK: PostgreSQL reporta ssl=on.' -ForegroundColor Green

# ============================================================
# 2. COMPROBAR CONEXION TLS REAL CON VERIFY-FULL
# ============================================================

Write-Host ''
Write-Host `
    'PostgreSQL TLS - verificando conexion real con verify-full...' `
    -ForegroundColor Cyan

$secureLines = & docker @compose exec -T `
    -e "PGPASSWORD=$postgresPassword" `
    -e 'PGSSLMODE=verify-full' `
    -e 'PGSSLROOTCERT=/var/lib/postgresql/tls/ca.crt' `
    postgres `
    psql `
    -h postgres `
    -U postgres `
    -d postgres `
    -tAc `
    "SELECT ssl::text || '|' || COALESCE(version,'') || '|' || COALESCE(cipher,'')
     FROM pg_stat_ssl
     WHERE pid = pg_backend_pid();" `
    2>&1

$secureExit = $LASTEXITCODE
$secure = ($secureLines | Out-String).Trim()

if (
    $secureExit -ne 0 -or
    $secure -notmatch '(?i)^(true|t)\|TLSv'
) {

    Write-Host `
        "ERROR: no se pudo validar una sesion PostgreSQL TLS verify-full. Respuesta: $secure" `
        -ForegroundColor Red

    exit 1
}

Write-Host "TLS negociado: $secure" -ForegroundColor Green

# ============================================================
# 3. COMPROBAR QUE POSTGRESQL RECHAZA TCP SIN TLS
# ============================================================

Write-Host ''
Write-Host `
    'PostgreSQL TLS - comprobando que TCP sin SSL sea rechazado...' `
    -ForegroundColor Cyan

# Esta conexion DEBE fallar.
# El stderr de psql se descarta DENTRO del contenedor para que
# Windows PowerShell 5.1 no convierta el rechazo esperado en
# NativeCommandError.

& docker @compose exec -T `
    -e "PGPASSWORD=$postgresPassword" `
    -e 'PGSSLMODE=disable' `
    postgres `
    sh -c `
    'psql -h postgres -U postgres -d postgres -tAc "SELECT 1;" >/dev/null 2>&1'

$plainExit = $LASTEXITCODE

if ($plainExit -eq 0) {

    Write-Host `
        'ERROR: PostgreSQL acepto una conexion TCP sin TLS; se esperaba rechazo.' `
        -ForegroundColor Red

    exit 1
}

Write-Host `
    'OK: PostgreSQL rechazo correctamente la conexion TCP sin TLS.' `
    -ForegroundColor Green

# ============================================================
# RESULTADO FINAL
# ============================================================

Write-Host ''
Write-Host `
    'OK: PostgreSQL exige TLS en TCP y verify-full funciona con la CA del proyecto.' `
    -ForegroundColor Green

exit 0
